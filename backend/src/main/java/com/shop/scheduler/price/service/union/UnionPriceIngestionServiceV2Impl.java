package com.shop.scheduler.price.service.union;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.admin.product.service.PriceErrorCardService;
import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;
import com.shop.card.entity.UnionPrice;
import com.shop.card.metadata.support.FabSetCode;
import com.shop.card.metadata.support.MtgImageKey;
import com.shop.card.repository.FabPriceRepository;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.card.repository.TcgPPriceRepository;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.common.util.UlidGenerator;
import com.shop.log.sync.event.SyncLogEvent;
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

@Primary
@Slf4j
@Service
@RequiredArgsConstructor
public class UnionPriceIngestionServiceV2Impl implements UnionPriceIngestionService {

    private static final int UPSERT_BATCH_SIZE = 1000;
    private static final int READ_PAGE_SIZE = 5000;
    private static final int BACKFILL_BATCH_SIZE = 1000;
    private static final int SEARCH_MAP_SYNC_CHUNK_SIZE = 2000;
    private static final String PRODUCT_TABLE_NAME = ProductTableEnum.UNION_PRICE.name();
    private static final String FAB_GAME = "Flesh & Blood TCG";
    private static final String MTG_GAME = "Magic: The Gathering";
    private static final String REBUILD_LOG_PREFIX = "[ProductSearchMapRebuild]";
    private static final String REBUILD_SYNC_LOG_TARGET = "product-search-map-reference-axis";
    private static final String REBUILD_SYNC_LOG_SOURCE = "admin";

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
                image_source = CASE
                    WHEN union_prices.image_source = 'SCRYFALL' THEN union_prices.image_source
                    ELSE VALUES(image_source)
                END,
                image_url = CASE
                    WHEN union_prices.image_source = 'SCRYFALL' THEN union_prices.image_url
                    ELSE VALUES(image_url)
                END,
                is_double_sided = VALUES(is_double_sided),
                public_id = CASE
                    WHEN public_id IS NULL OR public_id = '' THEN VALUES(public_id)
                    ELSE public_id
                END,
                set_number = VALUES(set_number)
            """;

    private static final String PRICE_SYNC_SEARCH_MAP_JOIN_CHUNK_SQL = """
            UPDATE product_search_maps p
            JOIN union_prices u ON u.id = p.source_id
            SET p.sort_price = u.price
            WHERE p.table_name = ?
            AND p.id > ?
            ORDER BY p.id
            LIMIT ?
            """;

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
    private final PriceErrorCardService priceErrorCardService;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean unionSaveRunning = new AtomicBoolean(false);
    private final AtomicBoolean backfillRunning = new AtomicBoolean(false);
    private volatile String rebuildPhase = "IDLE";
    private volatile int rebuildCurrentPage = 0;
    private volatile long rebuildProcessedRows = 0;
    private volatile String rebuildLastMessage = "";

    @Override
    public boolean startSaveAllPricesToUnion() {
        if (!unionSaveRunning.compareAndSet(false, true)) {
            log.warn("Union price save V2 is already running. Skip this request.");
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
        runUnionIngestionStep("search_map.sort_price", this::syncSearchMapSortPriceFromUnionInChunks);
        runUnionIngestionStep("product_search_maps", this::syncProductSearchMapFromUnionInPages);
        runUnionIngestionStep("price_error_visibility", priceErrorCardService::syncPriceErrorVisibility);
    }

    @Override
    public void saveOpenBinderPricesToUnion() {
        runUnionIngestionStep("fab_prices", this::saveFabPricesToUnionInPages);
        runUnionIngestionStep("mtg_prices", this::saveMtgPricesToUnionInPages);
        runUnionIngestionStep("search_map.sort_price", this::syncSearchMapSortPriceFromUnionInChunks);
        runUnionIngestionStep("product_search_maps", this::syncProductSearchMapFromUnionInPages);
        runUnionIngestionStep("price_error_visibility", priceErrorCardService::syncPriceErrorVisibility);
    }

    private void runUnionIngestionStep(String stepName, Runnable step) {
        try {
            step.run();
            log.info("Union ingestion V2 step completed: {}", stepName);
        } catch (Exception e) {
            log.error("Union ingestion V2 step failed: {}", stepName, e);
        }
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
            SetNameMap mappedSetName = setNormalizeService.findByGameAndName(FAB_GAME, fabPrice.getSetName());
            String normalizedSetName = mappedSetName != null ? mappedSetName.getName() : fabPrice.getSetName();
            String setNameWithEdition = appendEditionSuffixIfNeeded(normalizedSetName, fabPrice.getCheckCodeRefined());
            UnionPricePrintFieldMapper.FabPrintFields fabPrintFields =
                    UnionPricePrintFieldMapper.fromFabFoil(fabPrice.getFoil());
            unionPrices.add(UnionPrice.builder()
                    .game(FAB_GAME)
                    .productType("Cards")
                    .printType(fabPrintFields.printType())
                    .printing(fabPrintFields.printing())
                    .price(fabPrice.getPrice())
                    .setName(setNameWithEdition)
                    .setCode(FabSetCode.toDisplayCode(fabPrice.getSet()))
                    .cardName(fabPrice.getCardName())
                    .cardNameK(null)
                    .rarity(fabPrice.getRarity())
                    .checkCodeRefined(fabPrice.getCheckCodeRefined())
                    .imageSource("OPB")
                    .imageUrl(buildFabImageKey(fabPrice))
                    .isDoubleSided(false)
                    .setNumber(extractSetNumberFromCheckCodeRefined(fabPrice.getCheckCodeRefined()))
                    .build());
        }
        mergeAndSaveByCheckCodeRefined(unionPrices);
    }

    @Override
    @Transactional
    public void saveMtgPricesToUnion(List<MtgPrice> mtgPrices) {
        List<UnionPrice> unionPrices = new ArrayList<>();
        for (MtgPrice mtgPrice : mtgPrices) {
            if (mtgPrice == null || !hasUnionCheckCode(mtgPrice.getCheckCodeRefined())) {
                continue;
            }
            String imageKey = MtgImageKey.from(mtgPrice);
            if (imageKey.isBlank()) {
                continue;
            }
            String sourcePrintType = mtgPrice.getType();
            unionPrices.add(UnionPrice.builder()
                    .game(MTG_GAME)
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
                    .imageUrl(imageKey)
                    .isDoubleSided(Boolean.TRUE.equals(mtgPrice.getIsDoubleSided()))
                    .setNumber(extractSetNumberFromCheckCodeRefined(mtgPrice.getCheckCodeRefined()))
                    .build());
        }
        mergeAndSaveByCheckCodeRefined(unionPrices);
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
            if (game == null || game.isBlank() || tcgPrice.getProductId() == null || FAB_GAME.equalsIgnoreCase(game)) {
                continue;
            }
            String sourcePrintType = tcgPrice.getPrintType();
            SetNameMap mappedSetName = setNormalizeService.findByGameAndName(tcgPrice.getGame(), tcgPrice.getSet());
            unionPrices.add(UnionPrice.builder()
                    .game(game.equalsIgnoreCase("Magic") ? MTG_GAME : game)
                    .productType(tcgPrice.getType())
                    .printType(sourcePrintType)
                    .printing(tcgPrice.getPrinting())
                    .price(tcgPrice.getMarketPrice())
                    .setName(mappedSetName != null ? mappedSetName.getName() : tcgPrice.getSet())
                    .setCode(tcgPEdgeCaseSetCode(tcgPrice.getSetAbbrv()))
                    .cardName(tcgPrice.getProductName())
                    .cardNameK(null)
                    .rarity(tcgPrice.getRarity())
                    .checkCodeRefined(tcgPrice.getCheckCodeRefined())
                    .imageSource("TCGP")
                    .imageUrl(tcgPrice.getProductId().toString())
                    .isDoubleSided(tcgPrice.getIsDoubleSided())
                    .setNumber(extractSetNumberFromCheckCodeRefined(tcgPrice.getCheckCodeRefined()))
                    .build());
        }
        mergeAndSaveByCheckCodeRefined(unionPrices);
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
        if (tcgPrice == null || tcgPrice.getProductId() == null || FAB_GAME.equalsIgnoreCase(tcgPrice.getGame())) {
            return;
        }
        unionPriceRepository.updateImageMetaByCheckCodeRefined(
                tcgPrice.getCheckCodeRefined(),
                "TCGP",
                tcgPrice.getProductId().toString(),
                tcgPrice.getIsDoubleSided());
    }

    private void mergeAndSaveByCheckCodeRefined(List<UnionPrice> incoming) {
        List<UnionPrice> valid = incoming == null ? List.of()
                : incoming.stream().filter(u -> u != null && hasUnionCheckCode(u.getCheckCodeRefined())).toList();
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
            int[][] results = jdbcTemplate.batchUpdate(BACKFILL_PUBLIC_ID_UPDATE_SQL, targetIds, UPSERT_BATCH_SIZE,
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
        return updated;
    }

    @Override
    public boolean backfillProductSearchMapFromUnionPrices() {
        if (!backfillRunning.compareAndSet(false, true)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                syncProductSearchMapFromUnionInPages();
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
        rebuildPhase = "RUNNING";
        rebuildCurrentPage = 0;
        rebuildProcessedRows = 0;
        rebuildLastMessage = "ProductSearchMap reference-axis rebuild started";
        LocalDateTime startTime = LocalDateTime.now();
        CompletableFuture.runAsync(() -> {
            try {
                rebuildLastMessage = "Syncing union prices";
                syncProductSearchMapFromUnionInPages();
                rebuildLastMessage = "Syncing card products";
                syncProductSearchMapFromCardProductInPages();
                rebuildLastMessage = "Syncing sealed products";
                syncProductSearchMapFromSealedProductInPages();
                rebuildLastMessage = "Syncing manual products";
                syncProductSearchMapFromManualProductInPages();
                rebuildPhase = "IDLE";
                rebuildLastMessage = "ProductSearchMap reference-axis rebuild completed";
                log.info("{} ProductSearchMap reference-axis rebuild completed.", REBUILD_LOG_PREFIX);
                publishRebuildSyncLog(startTime, "success", rebuildLastMessage);
            } catch (Exception e) {
                rebuildPhase = "FAILED";
                rebuildLastMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("{} ProductSearchMap reference-axis rebuild failed", REBUILD_LOG_PREFIX, e);
                publishRebuildSyncLog(startTime, "failure", rebuildLastMessage);
            } finally {
                backfillRunning.set(false);
            }
        });
        return true;
    }

    @Override
    public ProductSearchMapRebuildStatus getProductSearchMapRebuildStatus() {
        return new ProductSearchMapRebuildStatus(
                backfillRunning.get(),
                rebuildPhase,
                rebuildCurrentPage,
                rebuildProcessedRows,
                rebuildLastMessage != null ? rebuildLastMessage : "");
    }

    private void publishRebuildSyncLog(LocalDateTime startTime, String result, String message) {
        eventPublisher.publishEvent(new SyncLogEvent(
                REBUILD_SYNC_LOG_TARGET,
                REBUILD_SYNC_LOG_SOURCE,
                startTime,
                LocalDateTime.now(),
                result,
                message));
    }

    private void syncProductSearchMapFromUnionInPages() {
        int pageNumber = 0;
        Page<UnionPrice> page;
        do {
            page = unionPriceRepository.findAll(
                    PageRequest.of(pageNumber, READ_PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")));
            productSearchMapService.syncProductSearchMapBulkByUnionPrice(page.getContent());
            trackRebuildPageProgress(pageNumber, page.getNumberOfElements());
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
            trackRebuildPageProgress(pageNumber, page.getNumberOfElements());
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
            trackRebuildPageProgress(pageNumber, page.getNumberOfElements());
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
            trackRebuildPageProgress(pageNumber, page.getNumberOfElements());
            pageNumber++;
        } while (page.hasNext());
    }

    private void trackRebuildPageProgress(int pageNumber, int rowCount) {
        if (!backfillRunning.get()) {
            return;
        }
        rebuildCurrentPage = pageNumber + 1;
        rebuildProcessedRows += rowCount;
    }

    @Transactional
    public void syncSearchMapSortPriceFromUnionInChunks() {
        long lastId = 0L;
        while (true) {
            int updated = jdbcTemplate.update(PRICE_SYNC_SEARCH_MAP_JOIN_CHUNK_SQL, PRODUCT_TABLE_NAME, lastId,
                    SEARCH_MAP_SYNC_CHUNK_SIZE);
            if (updated == 0) {
                break;
            }
            Long nextLastId = jdbcTemplate.queryForObject(PRICE_SYNC_SEARCH_MAP_NEXT_CURSOR_SQL, Long.class, lastId,
                    PRODUCT_TABLE_NAME, lastId, SEARCH_MAP_SYNC_CHUNK_SIZE);
            if (nextLastId == null || nextLastId <= lastId) {
                break;
            }
            lastId = nextLastId;
            if (updated < SEARCH_MAP_SYNC_CHUNK_SIZE) {
                break;
            }
        }
    }

    private String appendEditionSuffixIfNeeded(String setName, String checkCodeRefined) {
        if (setName == null || setName.isBlank() || checkCodeRefined == null || checkCodeRefined.isBlank()) {
            return setName;
        }
        String result = setName;
        String lowerCheckCodeRefined = checkCodeRefined.toLowerCase(Locale.ROOT);
        String lowerSetName = result.toLowerCase(Locale.ROOT);
        if (lowerCheckCodeRefined.contains("unlimited") && !lowerSetName.contains("(unlimited)")) {
            result += " (Unlimited)";
            lowerSetName = result.toLowerCase(Locale.ROOT);
        }
        if (lowerCheckCodeRefined.contains("1st edition") && !lowerSetName.contains("(1st edition)")) {
            result += " (1st Edition)";
        }
        return result;
    }

    private String buildFabImageKey(FabPrice fabPrice) {
        String set = FabSetCode.toSourceCode(fabPrice.getSet());
        String code = fabPrice.getCode() == null ? "" : fabPrice.getCode().trim();
        if (set.isBlank() || code.isBlank()) {
            return null;
        }
        return set + "-" + code;
    }

    private String mtgPEdgeCaseSetCode(String set) {
        return set;
    }

    private String tcgPEdgeCaseSetCode(String setAbbrv) {
        if (setAbbrv == null) {
            return "";
        }
        return switch (setAbbrv) {
            case "RVD/DVR" -> "RVD";
            default -> setAbbrv;
        };
    }

    private static boolean hasUnionCheckCode(String checkCodeRefined) {
        return checkCodeRefined != null && !checkCodeRefined.isBlank();
    }

    private Long extractSetNumberFromCheckCodeRefined(String checkCodeRefined) {
        if (checkCodeRefined == null || checkCodeRefined.isBlank()) {
            return null;
        }
        Pattern p = Pattern.compile("^[^-]*-(\\d+)");
        Matcher m = p.matcher(checkCodeRefined);
        if (m.find()) {
            return Long.parseLong(m.group(1));
        }
        return null;
    }
}
