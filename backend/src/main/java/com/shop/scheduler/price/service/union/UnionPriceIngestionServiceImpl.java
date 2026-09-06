package com.shop.scheduler.price.service.union;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;
import com.shop.card.entity.UnionPrice;
import com.shop.card.repository.FabPriceRepository;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.card.repository.TcgPPriceRepository;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.common.util.UlidGenerator;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.metadata.entity.SetNameMap;
import com.shop.product.metadata.service.SetNameMapService;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.service.ProductSearchMapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class UnionPriceIngestionServiceImpl implements UnionPriceIngestionService {

    private final FabPriceRepository fabPriceRepository;
    private final MtgPriceRepository mtgPriceRepository;
    private final TcgPPriceRepository tcgPriceRepository;
    private final UnionPriceRepository unionPriceRepository;
    private final CardProductRepository cardProductRepository;
    private final SealedProductRepository sealedProductRepository;
    private final ManualProductRepository manualProductRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SetNameMapService setNormalizeService;
    private final ProductSearchMapService productSearchMapService;
    private final AtomicBoolean unionSaveRunning = new AtomicBoolean(false);
    private final AtomicBoolean backfillRunning = new AtomicBoolean(false);
    private static final int UPSERT_BATCH_SIZE = 1000;
    private static final int READ_PAGE_SIZE = 5000;
    private static final int BACKFILL_BATCH_SIZE = 1000;
    private static final String PRODUCT_TABLE_NAME = ProductTableEnum.UNION_PRICE.name();

    //union_prices 테이블에 데이터 삽입 또는 업데이트
    private static final String UNION_PRICE_UPSERT_SQL = """
            INSERT INTO union_prices
            (game, product_type, print_type, printing, price, set_name, set_code, card_name, card_namek, rarity, check_code_refined, image_source, image_url, is_double_sided, public_id, set_number)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                game = VALUES(game),
                product_type = VALUES(product_type),
                print_type = VALUES(print_type),
                printing = VALUES(printing),
                price = VALUES(price),
                set_name = VALUES(set_name),
                set_code = VALUES(set_code),
                card_name = VALUES(card_name),
                card_namek = VALUES(card_namek),
                rarity = VALUES(rarity),
                check_code_refined = VALUES(check_code_refined),
                is_double_sided = VALUES(is_double_sided),
                public_id = CASE
                    WHEN public_id IS NULL OR public_id = '' THEN VALUES(public_id)
                    ELSE public_id
                END,
                set_number = VALUES(set_number)
            """;

    // product_search_maps.sort_price를 union_prices.price로 동기화할 때 사용할 청크 크기
    private static final int SEARCH_MAP_SYNC_CHUNK_SIZE = 2000;

    /**
     * [청크 업데이트]
     * - product_search_maps 와 union_prices를 source_id=id로 조인
     * - UNION_PRICE 타입만 대상으로 sort_price 갱신
     * - ORDER BY + LIMIT로 한 번에 너무 많은 행을 잠그지 않도록 제한
     */
    private static final String PRICE_SYNC_SEARCH_MAP_JOIN_CHUNK_SQL = """
            UPDATE product_search_maps p
            JOIN union_prices u ON u.id = p.source_id
            SET p.sort_price = u.price
            WHERE p.table_name = ?
            AND p.id > ?
            ORDER BY p.id
            LIMIT ?
            """;

    /**
     * [커서 전진용]
     * - "다음 청크 시작점(lastId)"를 계산
     * - 같은 조건(table_name, id > lastId)으로 LIMIT N 범위의 max(id)를 가져옴
     */
    private static final String PRICE_SYNC_SEARCH_MAP_NEXT_CURSOR_SQL = """
            SELECT COALESCE(MAX(t.id), ?) AS next_id
            FROM (
                SELECT p.id
                FROM product_search_maps p
                WHERE p.table_name = ?
                AND p.id > ?
                ORDER BY p.id
                LIMIT ?
            ) t
            """;
    private static final String BACKFILL_PUBLIC_ID_UPDATE_SQL = """
            UPDATE union_prices
            SET public_id = ?
            WHERE id = ?
              AND (public_id IS NULL OR TRIM(public_id) = '')
            """;




    @Override
    public boolean startSaveAllPricesToUnion() {
        if (!unionSaveRunning.compareAndSet(false, true)) {
            log.warn("Union price save is already running. Skip this request.");
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                saveAllPricesToUnion();
            } finally {
                unionSaveRunning.set(false);
            }
        });
        return true;
    }

    @Override
    public void saveAllPricesToUnion() {
        runUnionIngestionStep("tcg_p_prices", this::saveTcgPricesToUnionInPages);
        runUnionIngestionStep("fab_prices", this::saveFabPricesToUnionInPages);
        runUnionIngestionStep("mtg_prices", this::saveMtgPricesToUnionInPages);
        log.info("All source prices saved to union_prices (per-step errors are logged above).");
        runUnionIngestionStep("search_map.sort_price", this::syncSearchMapSortPriceFromUnionInChunks);
        runUnionIngestionStep("product_search_maps", this::syncProductSearchMapFromUnionInPages);
    }

    @Override
    public void saveOpenBinderPricesToUnion() {
        runUnionIngestionStep("fab_prices", this::saveFabPricesToUnionInPages);
        runUnionIngestionStep("mtg_prices", this::saveMtgPricesToUnionInPages);
        log.info("Open Binder source prices saved to union_prices.");
        runUnionIngestionStep("search_map.sort_price", this::syncSearchMapSortPriceFromUnionInChunks);
        runUnionIngestionStep("product_search_maps", this::syncProductSearchMapFromUnionInPages);
    }

    private void runUnionIngestionStep(String stepName, Runnable step) {
        try {
            step.run();
            log.info("Union ingestion step completed: {}", stepName);
        } catch (Exception e) {
            log.error("Union ingestion step failed: {}", stepName, e);
        }
    }

    @Override
    public boolean backfillProductSearchMapFromUnionPrices() {
        if (!backfillRunning.compareAndSet(false, true)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                syncProductSearchMapFromUnionInPages();
                log.info("Backfill ProductSearchMap from UnionPrices completed.");
            } catch (Exception e) {
                log.error("Error during backfill ProductSearchMap from UnionPrices", e);
            } finally {
                backfillRunning.set(false);
            }
        });
        return true;
    }

    @Override
    public boolean rebuildProductSearchMapsByReferenceAxis() {
        if (!backfillRunning.compareAndSet(false, true)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                syncProductSearchMapFromUnionInPages();
                syncProductSearchMapFromCardProductInPages();
                syncProductSearchMapFromSealedProductInPages();
                syncProductSearchMapFromManualProductInPages();
                log.info("ProductSearchMap reference-axis rebuild completed.");
            } catch (Exception e) {
                log.error("Error during ProductSearchMap reference-axis rebuild", e);
            } finally {
                backfillRunning.set(false);
            }
        });
        return true;
    }

    @Override
    public ProductSearchMapRebuildStatus getProductSearchMapRebuildStatus() {
        boolean running = backfillRunning.get();
        return new ProductSearchMapRebuildStatus(
                running,
                running ? "RUNNING" : "IDLE",
                0,
                0L,
                running ? "ProductSearchMap sync in progress" : "");
    }

    private void syncProductSearchMapFromUnionInPages() {
        int pageNumber = 0;
        Page<UnionPrice> page;
        do {
            page = unionPriceRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            productSearchMapService.syncProductSearchMapBulkByUnionPrice(page.getContent());
            pageNumber++;
        } while (page.hasNext());
        productSearchMapService.reconcileUnionPriceInStock();
    }

    private void syncProductSearchMapFromCardProductInPages() {
        int pageNumber = 0;
        Page<CardProduct> page;
        do {
            page = cardProductRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            productSearchMapService.syncProductSearchMapBulk(page.getContent());
            pageNumber++;
        } while (page.hasNext());
    }

    private void syncProductSearchMapFromSealedProductInPages() {
        int pageNumber = 0;
        Page<SealedProduct> page;
        do {
            page = sealedProductRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            page.getContent().forEach(productSearchMapService::syncProductSearchMap);
            pageNumber++;
        } while (page.hasNext());
    }

    private void syncProductSearchMapFromManualProductInPages() {
        int pageNumber = 0;
        Page<ManualProduct> page;
        do {
            page = manualProductRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            page.getContent().forEach(productSearchMapService::syncProductSearchMap);
            pageNumber++;
        } while (page.hasNext());
    }

    private void saveTcgPricesToUnionInPages() {
        int pageNumber = 0;
        Page<TcgPPrice> page;
        do {
            page = tcgPriceRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            saveTcgPricesToUnion(page.getContent());
            pageNumber++;
        } while (page.hasNext());
    }

    private void saveFabPricesToUnionInPages() {
        int pageNumber = 0;
        Page<FabPrice> page;
        do {
            page = fabPriceRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            saveFabPricesToUnion(page.getContent());
            pageNumber++;
        } while (page.hasNext());
    }

    private void saveMtgPricesToUnionInPages() {
        int pageNumber = 0;
        Page<MtgPrice> page;
        do {
            page = mtgPriceRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            saveMtgPricesToUnion(page.getContent());
            pageNumber++;
        } while (page.hasNext());
    }

    @Override
    @Transactional
    public void saveFabPricesToUnion(List<FabPrice> fabPrices) {
        List<UnionPrice> unionPrices = new ArrayList<>();
        for (FabPrice fabPrice : fabPrices) {
            if (fabPrice == null || !hasUnionCheckCode(fabPrice.getCheckCodeRefined())) {
                continue;
            }
            SetNameMap mappedSetName = setNormalizeService.findByGameAndName("Flesh & Blood TCG", fabPrice.getSetName());
            String normalizedSetName = mappedSetName != null ? mappedSetName.getName() : fabPrice.getSetName();
            String setNameWithEdition = appendEditionSuffixIfNeeded(normalizedSetName, fabPrice.getCheckCodeRefined());
            UnionPricePrintFieldMapper.FabPrintFields fabPrintFields =
                    UnionPricePrintFieldMapper.fromFabFoil(fabPrice.getFoil());
            UnionPrice unionPrice = UnionPrice.builder()
                    .game("Flesh & Blood TCG")
                    .productType("Cards")
                    .printType(fabPrintFields.printType())
                    .printing(fabPrintFields.printing())
                    .price(fabPrice.getPrice())
                    .setName(setNameWithEdition)
                    .setCode(fabPEdgeCaseSetCode(fabPrice.getSet()))
                    .cardName(fabPrice.getCardName())
                    .cardNameK(null)
                    .rarity(fabPrice.getRarity())
                    .checkCodeRefined(fabPrice.getCheckCodeRefined())
                    .imageSource("OPB")
                    .imageUrl(null)
                    .isDoubleSided(false)
                    .setNumber(extractSetNumberFromCheckCodeRefined(fabPrice.getCheckCodeRefined()))
                    .build();
            unionPrices.add(unionPrice);
        }
        mergeAndSaveByCheckCodeRefined(unionPrices);
    }

    private String appendEditionSuffixIfNeeded(String setName, String checkCodeRefined) {
        if (setName == null || setName.isBlank() || checkCodeRefined == null || checkCodeRefined.isBlank()) {
            return setName;
        }

        String result = setName;
        String lowerCheckCodeRefined = checkCodeRefined.toLowerCase();
        String lowerSetName = result.toLowerCase();

        if (lowerCheckCodeRefined.contains("unlimited") && !lowerSetName.contains("(unlimited)")) {
            result += " (Unlimited)";
            lowerSetName = result.toLowerCase();
        }
        if (lowerCheckCodeRefined.contains("1st edition") && !lowerSetName.contains("(1st edition)")) {
            result += " (1st Edition)";
        }

        return result;
    }

    // set 코드 정제(fab_prices)
    private String fabPEdgeCaseSetCode(String set) {
        String normalized = set == null ? "" : set.replaceAll("fabtcg", "");
        if (normalized.length() < 3) {
            return normalized;
        }
        set = normalized.substring(0, 3);
        switch (set) {
            case "RVD/DVR":
                return "RVD";
            case "DVR":
                return "RVD";
            default:
                return set;
        }
    }

    @Override
    @Transactional
    public void saveMtgPricesToUnion(List<MtgPrice> mtgPrices) {
        List<UnionPrice> unionPrices = new ArrayList<>();
        for (MtgPrice mtgPrice : mtgPrices) {
            if (mtgPrice == null || !hasUnionCheckCode(mtgPrice.getCheckCodeRefined())) {
                continue;
            }
            String setCode = mtgPrice.getSet();
            String cardCode = mtgPrice.getCode();
            if (setCode == null || setCode.isBlank() || cardCode == null || cardCode.isBlank()) {
                continue;
            }
            String sourcePrintType = mtgPrice.getType();
            UnionPrice unionPrice = UnionPrice.builder()
                    .game("Magic: The Gathering")
                    .productType("Cards")
                    .printType(sourcePrintType)
                    .printing(sourcePrintType)
                    .price(mtgPrice.getPrice())
                    .setName(mtgPrice.getSetName())
                    .setCode(mtgPEdgeCaseSetCode(mtgPrice.getSet()))
                    .cardName(mtgPrice.getName())
                    .cardNameK(mtgPrice.getNameK())
                    .rarity(mtgPrice.getRarity())
                    .checkCodeRefined(mtgPrice.getCheckCodeRefined())
                    .imageSource("OPB")
                    .imageUrl((setCode + "-" + cardCode).toLowerCase())
                    .isDoubleSided(Boolean.TRUE.equals(mtgPrice.getIsDoubleSided()))
                    .setNumber(extractSetNumberFromCheckCodeRefined(mtgPrice.getCheckCodeRefined()))
                    .build();
            unionPrices.add(unionPrice);
        }
        mergeAndSaveByCheckCodeRefined(unionPrices);
    }

    // set 코드 정제(mtg_prices)
    private String mtgPEdgeCaseSetCode(String set) {
        switch (set) {
            default:
                return set;
        }
    }

    @Override
    @Transactional
    public void saveTcgPricesToUnion(List<TcgPPrice> tcgPrices) {
        List<UnionPrice> unionPrices = new ArrayList<>();
        for (TcgPPrice tcgPrice : tcgPrices) {
            if (tcgPrice == null || !hasUnionCheckCode(tcgPrice.getCheckCodeRefined())) {
                continue;
            }
            String game = tcgPrice.getGame();
            if (game == null || game.isBlank() || tcgPrice.getProductId() == null) {
                continue;
            }
            String sourcePrintType = tcgPrice.getPrintType();
            UnionPrice unionPrice = UnionPrice.builder()
                    .game(game.equalsIgnoreCase("Magic") ? "Magic: The Gathering" : game)
                    .productType(tcgPrice.getType())
                    .printType(sourcePrintType)
                    .printing(tcgPrice.getPrinting())
                    .price(tcgPrice.getMarketPrice())
                    .setName(setNormalizeService.findByGameAndName(tcgPrice.getGame(), tcgPrice.getSet()) != null
                            ? setNormalizeService.findByGameAndName(tcgPrice.getGame(), tcgPrice.getSet())
                                    .getName()
                            : tcgPrice.getSet())
                    .setCode(tcgPEdgeCaseSetCode(tcgPrice.getSetAbbrv()))
                    .cardName(tcgPrice.getProductName())
                    .cardNameK(null)
                    .rarity(tcgPrice.getRarity())
                    .checkCodeRefined(tcgPrice.getCheckCodeRefined())
                    .imageSource("TCGP")
                    .imageUrl(tcgPrice.getProductId().toString())
                    .isDoubleSided(tcgPrice.getIsDoubleSided())
                    .setNumber(extractSetNumberFromCheckCodeRefined(tcgPrice.getCheckCodeRefined()))
                    .build();
            unionPrices.add(unionPrice);
        }
        mergeAndSaveByCheckCodeRefined(unionPrices);
    }

    // set 코드 정제(tcg_p_prices)
    private String tcgPEdgeCaseSetCode(String setAbbrv) {
        if (setAbbrv == null) {
            return "";
        }
        switch (setAbbrv) {
            case "RVD/DVR":
                return "RVD";
            default:
                return setAbbrv;
        }
    }

    private static boolean hasUnionCheckCode(String checkCodeRefined) {
        return checkCodeRefined != null && !checkCodeRefined.isBlank();
    }

    private void mergeAndSaveByCheckCodeRefined(List<UnionPrice> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        List<UnionPrice> valid = incoming.stream()
                .filter(u -> u != null && hasUnionCheckCode(u.getCheckCodeRefined()))
                .toList();
        if (valid.isEmpty()) {
            return;
        }
        for (int i = 0; i < valid.size(); i += UPSERT_BATCH_SIZE) {
            List<UnionPrice> chunk = valid.subList(i, Math.min(i + UPSERT_BATCH_SIZE, valid.size()));
            jdbcTemplate.batchUpdate(UNION_PRICE_UPSERT_SQL, chunk, UPSERT_BATCH_SIZE, (ps, unionPrice) -> {
                if (unionPrice.getPublicId() == null || unionPrice.getPublicId().isBlank()) {
                    unionPrice.setPublicId(UlidGenerator.nextUlid());
                }
                ps.setString(1, unionPrice.getGame());
                ps.setString(2, unionPrice.getProductType());
                ps.setString(3, unionPrice.getPrintType());
                ps.setString(4, unionPrice.getPrinting());
                ps.setBigDecimal(5, unionPrice.getPrice());
                ps.setString(6, unionPrice.getSetName());
                ps.setString(7, unionPrice.getSetCode());
                ps.setString(8, unionPrice.getCardName());
                ps.setString(9, unionPrice.getCardNameK());
                ps.setString(10, unionPrice.getRarity());
                ps.setString(11, unionPrice.getCheckCodeRefined());
                ps.setString(12, unionPrice.getImageSource());
                ps.setString(13, unionPrice.getImageUrl());
                ps.setObject(14, unionPrice.getIsDoubleSided(), Types.BOOLEAN);
                ps.setString(15, unionPrice.getPublicId());
                ps.setObject(16, unionPrice.getSetNumber(), Types.BIGINT);
            });
        }
    }

    @Override
    @Transactional
    public int backfillMissingPublicIds() {
        long lastId = 0L;
        int updated = 0;

        while (true) {
            List<Long> targetIds = unionPriceRepository.findPublicIdBackfillTargetIds(lastId, BACKFILL_BATCH_SIZE);

            if (targetIds.isEmpty()) {
                break;
            }

            int[][] results = jdbcTemplate.batchUpdate(
                    BACKFILL_PUBLIC_ID_UPDATE_SQL,
                    targetIds,
                    UPSERT_BATCH_SIZE,
                    (ps, id) -> {
                        ps.setString(1, UlidGenerator.nextUlid());
                        ps.setLong(2, id);
                    });
            for (int[] batch : results) {
                for (int result : batch) {
                    if (result > 0) {
                        updated += result;
                    }
                }
            }
            lastId = targetIds.get(targetIds.size() - 1);
        }

        log.info("UnionPrice public_id backfill completed. updated={}", updated);
        return updated;
    }

    @Override
    @Transactional
    public void saveTcgPricesImageUrlToUnion(List<TcgPPrice> tcgPrices) {
        for (TcgPPrice tcgPrice : tcgPrices) {
            saveTcgPriceImageUrlToUnion(tcgPrice);
        }
    }

    @Override
    @Transactional
    public void saveTcgPriceImageUrlToUnion(TcgPPrice tcgPrice) {
        if (tcgPrice == null || tcgPrice.getProductId() == null) {
            return;
        }
        unionPriceRepository.updateImageMetaByCheckCodeRefined(
                tcgPrice.getCheckCodeRefined(),
                "TCGP",
                tcgPrice.getProductId().toString(),
                tcgPrice.getIsDoubleSided());
    }

    /**
 * union_prices.price -> product_search_maps.sort_price 동기화
 *
 * 왜 청크로 나누나?
 * - 10만건 이상을 한 번에 UPDATE하면 락/트랜잭션 부담이 큼
 * - ORDER BY id LIMIT N 으로 나누면 운영 안정성이 좋아짐
 */
@Transactional
public void syncSearchMapSortPriceFromUnionInChunks() {
    long lastId = 0L;
    int totalUpdated = 0;
    int loop = 0;

    while (true) {
        // 1) 현재 커서 이후 N건만 조인 업데이트
        int updated = jdbcTemplate.update(
                PRICE_SYNC_SEARCH_MAP_JOIN_CHUNK_SQL,
                PRODUCT_TABLE_NAME, // table_name = 'UNION_PRICE'
                lastId,             // p.id > lastId
                SEARCH_MAP_SYNC_CHUNK_SIZE);

        if (updated == 0) {
            // 더 이상 갱신 대상이 없으면 종료
            break;
        }

        totalUpdated += updated;
        loop++;

        // 2) 다음 커서(lastId) 계산
        Long nextLastId = jdbcTemplate.queryForObject(
                PRICE_SYNC_SEARCH_MAP_NEXT_CURSOR_SQL,
                Long.class,
                lastId,                 // COALESCE fallback
                PRODUCT_TABLE_NAME,     // table_name 필터
                lastId,                 // p.id > lastId
                SEARCH_MAP_SYNC_CHUNK_SIZE);

        if (nextLastId == null || nextLastId <= lastId) {
            // 비정상 커서 방지용 안전장치
            log.warn("Stop syncSearchMapSortPriceFromUnionInChunks: invalid cursor. lastId={}, nextLastId={}",
                    lastId, nextLastId);
            break;
        }

        lastId = nextLastId;

        log.info("search_map sort_price sync chunk done. loop={}, updated={}, lastId={}", loop, updated, lastId);

        // 마지막 청크(업데이트 건수가 chunk보다 작음)면 종료
        if (updated < SEARCH_MAP_SYNC_CHUNK_SIZE) {
            break;
        }
    }

    log.info("search_map sort_price sync completed. totalUpdated={}, loops={}", totalUpdated, loop);
}

private Long extractSetNumberFromCheckCodeRefined(String checkCodeRefined) {
    if (checkCodeRefined == null || checkCodeRefined.isBlank()) {
        return null;
    }
    // 체크 코드 리파인드에서 세트 번호만 추출. 추출 조건: 첫번쨰 '-' 이후의 숫자만 추출.
    Pattern p = Pattern.compile("^[^-]*-(\\d+)");
    Matcher m = p.matcher(checkCodeRefined);
    if (m.find()) {
        return Long.parseLong(m.group(1));
    }
    return null;
}
}
