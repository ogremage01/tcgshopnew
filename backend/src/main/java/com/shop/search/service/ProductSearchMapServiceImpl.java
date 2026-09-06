package com.shop.search.service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.card.entity.UnionPrice;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.product.dto.ProductItemDto;
import com.shop.product.enums.GameEnum;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.mapper.ProductItemDtoMapper;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.product.dto.GameSalesInfoDto;
import com.shop.product.service.GameSalesInfoService;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.dto.searching.ProductSearchingDto;
import com.shop.search.dto.searching.SearchFacetsBundle;
import com.shop.search.dto.searching.SearchInitResponseDto;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchMapServiceImpl implements ProductSearchMapService {

    private static final String LOG_PREFIX = "[PSM-STOCK]";

    /** {@link com.shop.product.mapper.ProductItemDtoMapper} 의 수동 상품 productType 과 동일 */
    private static final String PRODUCT_TYPE_MANUAL_PRODUCTS = "ManualProducts";
    private static final String PRODUCT_TYPE_SUPPLIES = "Supplies";

    private final ProductSearchMapRepository productSearchMapRepository;
    private final UnionPriceRepository unionPriceRepository;
    private final GameSalesInfoService gameSalesInfoService;
    private final CardProductRepository cardProductRepository;
    private final ProductItemDtoMapper productItemDtoMapper;
    private final ManualProductRepository manualProductRepository;
    private final SealedProductRepository sealedProductRepository;
    private final SupplyRepository supplyRepository;
    private final CachedSearchFacetLoader cachedSearchFacetLoader;
    //------------------------------------------------
    // 동기화용 메서드
    //------------------------------------------------
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMap(CardProduct cardProduct) {
        if (cardProduct == null) {
            log.warn("{} syncProductSearchMap(CardProduct) skipped: null entity", LOG_PREFIX);
            return;
        }
        log.info(
                "{} syncProductSearchMap(CardProduct) enter id={} publicId={} argStock={}",
                LOG_PREFIX,
                cardProduct.getId(),
                cardProduct.getPublicId(),
                cardProduct.getCurrentVisibleStock());
        upsertByCardProduct(cardProduct);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMapBulk(List<CardProduct> cardProducts) {
        if (cardProducts == null || cardProducts.isEmpty()) {
            return;
        }

        List<Long> cardProductIds = cardProducts.stream()
                .filter(Objects::nonNull)
                .map(CardProduct::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (cardProductIds.isEmpty()) {
            return;
        }

        List<CardProduct> sources = cardProductRepository.findByIdInWithUnionPrice(cardProductIds).stream()
                .filter(source -> source.getPublicId() != null && !source.getPublicId().isBlank())
                .filter(source -> source.getUnionPrice() != null)
                .toList();
        if (sources.isEmpty()) {
            return;
        }

        List<String> productIds = sources.stream()
                .map(CardProduct::getPublicId)
                .distinct()
                .toList();

        Map<String, ProductSearchMap> existingByProductId = productSearchMapRepository
                .findByTableNameAndProductIdIn(ProductTableEnum.CARD_PRODUCT, productIds)
                .stream()
                .collect(Collectors.toMap(ProductSearchMap::getProductId, map -> map, (a, b) -> a));

        List<ProductSearchMap> toSave = new ArrayList<>(sources.size());
        for (CardProduct source : sources) {
            ProductSearchMap searchMap = existingByProductId.computeIfAbsent(
                    source.getPublicId(), ignored -> new ProductSearchMap());
            populateSearchMapFromCardProduct(searchMap, source);
            toSave.add(searchMap);
        }

        productSearchMapRepository.saveAll(toSave);
        syncUnionPriceAggregateSearchMapsBulk(sources);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMap(SealedProduct sealedProduct) {
        if (sealedProduct == null) {
            log.warn("{} syncProductSearchMap(SealedProduct) skipped: null entity", LOG_PREFIX);
            return;
        }
        log.info(
                "{} syncProductSearchMap(SealedProduct) enter id={} publicId={} argStock={}",
                LOG_PREFIX,
                sealedProduct.getId(),
                sealedProduct.getPublicId(),
                sealedProduct.getCurrentVisibleStock());
        upsertBySealedProduct(sealedProduct);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMap(ManualProduct manualProduct) {
        if (manualProduct == null) {
            log.warn("{} syncProductSearchMap(ManualProduct) skipped: null entity", LOG_PREFIX);
            return;
        }
        log.info(
                "{} syncProductSearchMap(ManualProduct) enter id={} publicId={} argStock={}",
                LOG_PREFIX,
                manualProduct.getId(),
                manualProduct.getPublicId(),
                manualProduct.getStock());
        upsertByManualProduct(manualProduct);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMap(Supply supply) {
        if (supply == null) {
            log.warn("{} syncProductSearchMap(Supply) skipped: null entity", LOG_PREFIX);
            return;
        }
        log.info(
                "{} syncProductSearchMap(Supply) enter id={} publicId={} argStock={}",
                LOG_PREFIX,
                supply.getId(),
                supply.getPublicId(),
                supply.getStock());
        upsertBySupply(supply);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMap(UnionPrice unionPrice) {
        upsertByUnionPrice(unionPrice);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncProductSearchMapBulkByUnionPrice(List<UnionPrice> unionPrices) {
        if (unionPrices == null || unionPrices.isEmpty()) {
            return;
        }

        Map<String, UnionPrice> eligibleByPublicId = new LinkedHashMap<>();
        for (UnionPrice unionPrice : unionPrices) {
            if (!isEligibleUnionPriceRepresentative(unionPrice)) {
                continue;
            }
            eligibleByPublicId.putIfAbsent(unionPrice.getPublicId(), unionPrice);
        }
        if (eligibleByPublicId.isEmpty()) {
            return;
        }

        List<UnionPrice> eligible = List.copyOf(eligibleByPublicId.values());
        List<String> productIds = List.copyOf(eligibleByPublicId.keySet());
        List<Long> unionPriceIds = eligible.stream()
                .map(UnionPrice::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, ProductSearchMap> existingByProductId = productSearchMapRepository
                .findByTableNameAndProductIdIn(ProductTableEnum.UNION_PRICE, productIds)
                .stream()
                .collect(Collectors.toMap(ProductSearchMap::getProductId, map -> map, (a, b) -> a));

        Set<Long> unionPriceIdsWithStock = resolveUnionPriceIdsWithVisibleStock(unionPriceIds);

        List<ProductSearchMap> toSave = new ArrayList<>(eligible.size());
        for (UnionPrice unionPrice : eligible) {
            ProductSearchMap searchMap = existingByProductId.computeIfAbsent(
                    unionPrice.getPublicId(), ignored -> new ProductSearchMap());
            populateSearchMapFromUnionPrice(
                    searchMap,
                    unionPrice,
                    unionPriceIdsWithStock.contains(unionPrice.getId()));
            toSave.add(searchMap);
        }

        productSearchMapRepository.saveAll(toSave);
    }

    private void upsertByUnionPrice(UnionPrice unionPrice) {
        if (!isEligibleUnionPriceRepresentative(unionPrice)) {
            return;
        }
        ProductSearchMap searchMap = productSearchMapRepository
                .findByProductIdAndTableName(unionPrice.getPublicId(), ProductTableEnum.UNION_PRICE)
                .orElseGet(ProductSearchMap::new);

        populateSearchMapFromUnionPrice(searchMap, unionPrice, hasVisibleCardProductStock(unionPrice.getId()));
        productSearchMapRepository.save(searchMap);
    }

    private boolean isCardTypeUnionPrice(UnionPrice unionPrice) {
        if (unionPrice == null || unionPrice.getPublicId() == null) {
            return false;
        }
        String productType = unionPrice.getProductType();
        return productType == null
                || productType.isBlank()
                || productType.toLowerCase().startsWith("cards");
    }

    private boolean isEligibleUnionPriceRepresentative(UnionPrice unionPrice) {
        if (!isCardTypeUnionPrice(unionPrice)) {
            return false;
        }
        String cardName = unionPrice.getCardName();
        return cardName == null || !cardName.contains("(Art)");
    }

    private Set<Long> resolveUnionPriceIdsWithVisibleStock(List<Long> unionPriceIds) {
        if (unionPriceIds == null || unionPriceIds.isEmpty()) {
            return Set.of();
        }
        return cardProductRepository.findVisibleWithUnionPriceByUnionPriceIds(unionPriceIds).stream()
                .filter(offer -> {
                    Long stock = offer.getCurrentVisibleStock();
                    return stock != null && stock > 0;
                })
                .map(offer -> offer.getUnionPrice().getId())
                .collect(Collectors.toSet());
    }

    private void populateSearchMapFromUnionPrice(
            ProductSearchMap searchMap,
            UnionPrice unionPrice,
            boolean inStock) {
        searchMap.setProductName(unionPrice.getCardName());
        searchMap.setProductNameKo(unionPrice.getCardNameK());
        searchMap.setProductType(unionPrice.getProductType());
        searchMap.setGame(unionPrice.getGame());
        searchMap.setSetCode(unionPrice.getSetCode());
        searchMap.setPrintType(unionPrice.getPrintType());
        searchMap.setInStock(inStock);
        searchMap.setIsVisible(true);
        searchMap.setSortPrice(unionPrice.getPrice());
        searchMap.setProductId(unionPrice.getPublicId());
        searchMap.setSourceId(unionPrice.getId());
        searchMap.setCatalogSourceId(unionPrice.getId());
        searchMap.setSourcePublicId(unionPrice.getPublicId());
        searchMap.setTableName(ProductTableEnum.UNION_PRICE);
    }

    private void upsertByManualProduct(ManualProduct manualProduct) {
        if (manualProduct == null || manualProduct.getId() == null) {
            log.warn("ManualProduct search map sync skipped: missing entity or id");
            return;
        }
        ManualProduct source = manualProductRepository.findById(manualProduct.getId()).orElse(null);
        if (source == null) {
            log.warn("{} upsertByManualProduct skipped: ManualProduct not found id={}", LOG_PREFIX, manualProduct.getId());
            return;
        }
        String publicId = source.getPublicId();
        if (publicId == null || publicId.isBlank()) {
            log.warn("ManualProduct search map sync skipped: missing publicId (id={})", source.getId());
            return;
        }

        ProductSearchMap searchMap = productSearchMapRepository
                .findByProductIdAndTableName(publicId, ProductTableEnum.MANUAL_PRODUCT)
                .orElseGet(ProductSearchMap::new);

        long stock = source.getStock() != null ? source.getStock() : 0L;
        long price = source.getPrice() != null ? source.getPrice() : 0L;
        boolean visible = Boolean.TRUE.equals(source.getIsVisible())
                && !Boolean.TRUE.equals(source.getIsDeleted());

        searchMap.setProductName(source.getNameEn());
        searchMap.setProductNameKo(source.getNameKo());
        // 검색 facet·필터용 대분류 — ProductItemDto.productType 과 동일
        searchMap.setProductType(PRODUCT_TYPE_MANUAL_PRODUCTS);
        searchMap.setManualCategory(source.getProductType());
        searchMap.setGame(source.getProductIp());
        searchMap.setInStock(stock > 0);
        searchMap.setIsVisible(visible);
        searchMap.setSortPrice(BigDecimal.valueOf(price));
        searchMap.setProductId(publicId);
        searchMap.setSourceId(source.getId());
        searchMap.setCatalogSourceId(null);
        searchMap.setSourcePublicId(publicId);
        searchMap.setTableName(ProductTableEnum.MANUAL_PRODUCT);

        ProductSearchMap saved = productSearchMapRepository.save(searchMap);
        log.info(
                "{} upsertByManualProduct saved searchMapId={} productId={} inStockAfter={} stockUsed={}",
                LOG_PREFIX,
                saved.getId(),
                saved.getProductId(),
                saved.getInStock(),
                stock);
    }

    private void upsertBySupply(Supply supply) {
        if (supply == null || supply.getId() == null) {
            log.warn("{} upsertBySupply skipped: missing Supply or id", LOG_PREFIX);
            return;
        }
        Supply source = supplyRepository.findById(supply.getId()).orElse(null);
        if (source == null) {
            log.warn("{} upsertBySupply skipped: Supply not found id={}", LOG_PREFIX, supply.getId());
            return;
        }
        String publicId = source.getPublicId();
        if (publicId == null || publicId.isBlank()) {
            log.warn("{} upsertBySupply skipped: missing publicId id={}", LOG_PREFIX, source.getId());
            return;
        }

        ProductSearchMap searchMap = productSearchMapRepository
                .findByProductIdAndTableName(publicId, ProductTableEnum.SUPPLY)
                .orElseGet(ProductSearchMap::new);

        long stock = source.getStock() != null ? source.getStock() : 0L;
        long price = source.getPrice() != null ? source.getPrice() : 0L;
        boolean visible = Boolean.TRUE.equals(source.getIsVisible())
                && !Boolean.TRUE.equals(source.getIsDeleted());

        searchMap.setProductName(source.getNameEn());
        searchMap.setProductNameKo(source.getNameKo());
        searchMap.setProductType(PRODUCT_TYPE_SUPPLIES);
        searchMap.setSuppliesType(source.getSupplyType());
        searchMap.setInStock(stock > 0);
        searchMap.setIsVisible(visible);
        searchMap.setSortPrice(BigDecimal.valueOf(price));
        searchMap.setProductId(publicId);
        searchMap.setSourceId(source.getId());
        searchMap.setCatalogSourceId(null);
        searchMap.setSourcePublicId(publicId);
        searchMap.setTableName(ProductTableEnum.SUPPLY);

        ProductSearchMap saved = productSearchMapRepository.save(searchMap);
        log.info(
                "{} upsertBySupply saved searchMapId={} productId={} inStockAfter={} stockUsed={}",
                LOG_PREFIX,
                saved.getId(),
                saved.getProductId(),
                saved.getInStock(),
                stock);
    }

    private boolean hasVisibleCardProductStock(Long unionPriceId) {
        if (unionPriceId == null) {
            return false;
        }
        return cardProductRepository.findVisibleWithUnionPriceByUnionPriceIds(List.of(unionPriceId)).stream()
                .anyMatch(offer -> {
                    Long stock = offer.getCurrentVisibleStock();
                    return stock != null && stock > 0;
                });
    }

    private void upsertBySealedProduct(SealedProduct sealedProduct) {
        if (sealedProduct == null || sealedProduct.getId() == null) {
            log.warn("{} upsertBySealedProduct skipped: missing SealedProduct or id", LOG_PREFIX);
            return;
        }
        SealedProduct source = sealedProductRepository.findById(sealedProduct.getId()).orElse(null);
        if (source == null) {
            log.warn("{} upsertBySealedProduct skipped: SealedProduct not found id={}", LOG_PREFIX, sealedProduct.getId());
            return;
        }
        String publicId = source.getPublicId();
        if (publicId == null || publicId.isBlank()) {
            log.warn("{} upsertBySealedProduct skipped: missing publicId id={}", LOG_PREFIX, source.getId());
            return;
        }

        ProductSearchMap searchMap = productSearchMapRepository
                .findByProductIdAndTableName(publicId, ProductTableEnum.SEALED_PRODUCT)
                .orElseGet(ProductSearchMap::new);

        long stock = source.visibleStock();
        boolean visible = Boolean.TRUE.equals(source.getIsActive()) && !Boolean.TRUE.equals(source.getIsDeleted());

        searchMap.setProductName(source.getProductNameEn());
        searchMap.setProductNameKo(source.getProductNameKo());
        searchMap.setProductType("SealedProducts");
        searchMap.setGame(source.getGame());
        searchMap.setSetCode(source.getSetCode());
        searchMap.setInStock(stock > 0);
        searchMap.setIsVisible(visible);
        searchMap.setSortPrice(BigDecimal.valueOf(source.getPrice() == null ? 0L : source.getPrice()));
        searchMap.setProductId(publicId);
        searchMap.setSourceId(source.getId());
        searchMap.setCatalogSourceId(null);
        searchMap.setSourcePublicId(publicId);
        searchMap.setTableName(ProductTableEnum.SEALED_PRODUCT);

        ProductSearchMap saved = productSearchMapRepository.save(searchMap);
        log.info(
                "{} upsertBySealedProduct saved searchMapId={} productId={} inStockAfter={} stockUsed={}",
                LOG_PREFIX,
                saved.getId(),
                saved.getProductId(),
                saved.getInStock(),
                stock);
    }

    //------------------------------------------------
    // 관리용 메서드
    //------------------------------------------------
    private void upsertByCardProduct(CardProduct cardProduct) {
        if (cardProduct == null || cardProduct.getId() == null) {
            log.warn("{} upsertByCardProduct skipped: missing CardProduct or id", LOG_PREFIX);
            return;
        }
        Long argStock = cardProduct.getCurrentVisibleStock();
        CardProduct source = cardProductRepository.findByIdWithUnionPrice(cardProduct.getId())
                .or(() -> cardProductRepository.findById(cardProduct.getId()))
                .orElse(null);
        if (source == null) {
            log.warn("{} upsertByCardProduct skipped: CardProduct not found id={}", LOG_PREFIX, cardProduct.getId());
            return;
        }
        if (source.getUnionPrice() == null) {
            log.warn("{} upsertByCardProduct skipped: UnionPrice missing cardProductId={}", LOG_PREFIX, source.getId());
            return;
        }
        if (source.getPublicId() == null || source.getPublicId().isBlank()) {
            log.warn("{} upsertByCardProduct skipped: missing publicId cardProductId={}", LOG_PREFIX, source.getId());
            return;
        }

        String publicId = source.getPublicId();
        long visibleStock = source.getCurrentVisibleStock() != null ? source.getCurrentVisibleStock() : 0L;

        ProductSearchMap searchMap = productSearchMapRepository
                .findByProductIdAndTableName(publicId, ProductTableEnum.CARD_PRODUCT)
                .orElseGet(ProductSearchMap::new);

        Boolean inStockBefore = searchMap.getInStock();
        Long searchMapIdBefore = searchMap.getId();
        boolean isNewRow = searchMapIdBefore == null;

        log.info(
                "{} upsertByCardProduct lookup productId={} searchMapId={} isNewRow={} inStockBefore={} "
                        + "argStock={} dbStock={}",
                LOG_PREFIX,
                publicId,
                searchMapIdBefore,
                isNewRow,
                inStockBefore,
                argStock,
                visibleStock);

        populateSearchMapFromCardProduct(searchMap, source);

        ProductSearchMap saved = productSearchMapRepository.save(searchMap);
        log.info(
                "{} upsertByCardProduct saved searchMapId={} productId={} inStockBefore={} inStockAfter={} "
                        + "currentVisibleStockUsed={}",
                LOG_PREFIX,
                saved.getId(),
                saved.getProductId(),
                inStockBefore,
                saved.getInStock(),
                visibleStock);

        syncUnionPriceAggregateSearchMapsBulk(List.of(source));
    }

    private void populateSearchMapFromCardProduct(ProductSearchMap searchMap, CardProduct source) {
        UnionPrice unionPrice = source.getUnionPrice();
        long visibleStock = source.getCurrentVisibleStock() != null ? source.getCurrentVisibleStock() : 0L;

        searchMap.setProductName(unionPrice.getCardName());
        searchMap.setProductNameKo(unionPrice.getCardNameK());
        searchMap.setProductType(source.getProductType());
        searchMap.setGame(unionPrice.getGame());
        searchMap.setSetCode(unionPrice.getSetCode());
        searchMap.setPrintType(source.getPrintType());
        searchMap.setInStock(visibleStock > 0);
        searchMap.setIsVisible(source.getIsVisible());
        searchMap.setSortPrice(unionPrice.getPrice());
        searchMap.setProductId(source.getPublicId());
        searchMap.setSourceId(source.getId());
        searchMap.setCatalogSourceId(unionPrice.getId());
        searchMap.setSourcePublicId(source.getPublicId());
        searchMap.setTableName(ProductTableEnum.CARD_PRODUCT);
    }

    private void syncUnionPriceAggregateSearchMapsBulk(List<CardProduct> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }

        Map<Long, UnionPrice> unionPriceById = new LinkedHashMap<>();
        for (CardProduct source : sources) {
            if (source == null || source.getUnionPrice() == null) {
                continue;
            }
            UnionPrice unionPrice = source.getUnionPrice();
            if (unionPrice.getId() == null || unionPrice.getPublicId() == null) {
                continue;
            }
            if (!isEligibleUnionPriceRepresentative(unionPrice)) {
                continue;
            }
            unionPriceById.putIfAbsent(unionPrice.getId(), unionPrice);
        }
        if (unionPriceById.isEmpty()) {
            return;
        }

        List<UnionPrice> unionPrices = List.copyOf(unionPriceById.values());
        List<Long> unionPriceIds = List.copyOf(unionPriceById.keySet());
        List<String> unionPublicIds = unionPrices.stream()
                .map(UnionPrice::getPublicId)
                .distinct()
                .toList();

        Set<Long> unionPriceIdsWithStock = resolveUnionPriceIdsWithVisibleStock(unionPriceIds);
        Map<String, ProductSearchMap> existingByProductId = productSearchMapRepository
                .findByTableNameAndProductIdIn(ProductTableEnum.UNION_PRICE, unionPublicIds)
                .stream()
                .collect(Collectors.toMap(ProductSearchMap::getProductId, map -> map, (a, b) -> a));

        List<ProductSearchMap> toSave = new ArrayList<>(unionPrices.size());
        for (UnionPrice unionPrice : unionPrices) {
            ProductSearchMap aggregateMap = existingByProductId.computeIfAbsent(
                    unionPrice.getPublicId(), ignored -> new ProductSearchMap());
            populateSearchMapFromUnionPrice(
                    aggregateMap,
                    unionPrice,
                    unionPriceIdsWithStock.contains(unionPrice.getId()));
            toSave.add(aggregateMap);
        }

        productSearchMapRepository.saveAll(toSave);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void isvisibleChangeProductSearchMapByStorage(Long storageId) {
        productSearchMapRepository.updateByStorageIdAndIsVisible(storageId);
        List<CardProduct> cardProducts = cardProductRepository.findByStorageId(storageId);
        if (!cardProducts.isEmpty()) {
            syncUnionPriceAggregateSearchMapsBulk(cardProducts);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconcileUnionPriceInStock() {
        int updated = productSearchMapRepository.reconcileUnionPriceInStock();
        log.info("{} reconcileUnionPriceInStock updatedRows={}", LOG_PREFIX, updated);
    }

    //------------------------------------------------
    // 검색용 메서드
    //------------------------------------------------

    /**
     * 검색 초기 응답: 첫 페이지 상품 + facet(필터 후보).
     * facet은 {@code ProductSearchingDto} 전체 조건과 동일한 WHERE(QueryDSL)에서만 뽑아
     * 상품 목록과 정의역을 맞춘다.
     */
    @Override
    @Transactional(readOnly = true)
    public SearchInitResponseDto searchInit(ProductSearchingDto query, Pageable pageable) {
        long t0 = System.nanoTime();
        Page<ProductItemDto> firstPage = doSearchProducts(query, pageable);
        long msProducts = (System.nanoTime() - t0) / 1_000_000L;

        t0 = System.nanoTime();
        SearchFacetsBundle facets = cachedSearchFacetLoader.loadFacetsParallel(query);
        long msFacets = (System.nanoTime() - t0) / 1_000_000L;

        long totalMs = msProducts + msFacets;
        if (totalMs >= 500L) {
            log.info(
                    "search perf: searchInit total={}ms (products={}ms, facets={}ms, keyword={})",
                    totalMs,
                    msProducts,
                    msFacets,
                    query.getKeyword() != null && !query.getKeyword().isBlank());
        } else if (log.isDebugEnabled()) {
            log.debug(
                    "search perf: searchInit total={}ms (products={}ms, facets={}ms)",
                    totalMs,
                    msProducts,
                    msFacets);
        }

        return SearchInitResponseDto.builder()
                .products(firstPage)
                .games(facets.games())
                .productTypes(facets.productTypes())
                .suppliesTypes(facets.suppliesTypes())
                .manualCategories(facets.manualCategories())
                .printTypes(facets.printTypes())
                .rarities(facets.rarities())
                .setNames(facets.setNames())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductItemDto> searchProducts(ProductSearchingDto query, Pageable pageable) {
        long t0 = System.nanoTime();
        Page<ProductItemDto> result = doSearchProducts(query, pageable);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        if (ms >= 500L) {
            log.info("search perf: searchProducts {}ms", ms);
        } else if (log.isDebugEnabled()) {
            log.debug("search perf: searchProducts {}ms", ms);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductItemDto> searchProductsByGameSetCode(
            String game,
            String setCode,
            ProductSearchingDto query,
            Pageable pageable) {
        if (game == null || game.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        GameEnum gameEnum = GameEnum.fromCode(game);
        if (gameEnum == GameEnum.OTHER && !GameEnum.OTHER.getGameAbbr().equalsIgnoreCase(game.trim())) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        ProductSearchingDto merged = ProductSearchingDto.builder()
                .keyword(query.getKeyword())
                .games(List.of(gameEnum.getGame()))
                .productTypes(query.getProductTypes())
                .suppliesTypes(query.getSuppliesTypes())
                .manualCategories(query.getManualCategories())
                .rarities(query.getRarities())
                .setNames(query.getSetNames())
                .searchMode(query.getSearchMode())
                .entryState(query.getEntryState())
                .isFoil(query.getIsFoil())
                .isInStock(query.getIsInStock())
                .setCode(setCode)
                .build();

        return doSearchProducts(merged, pageable);
    }
    // 검색 결과 조회

    private Page<ProductItemDto> doSearchProducts(ProductSearchingDto query, Pageable pageable) {
        Page<ProductSearchMap> mapPage = productSearchMapRepository.findByProductSearchingDto(query, pageable);
        List<ProductSearchMap> groupRows = mapPage.getContent();

        List<Long> unionPriceIds = groupRows.stream()
                .filter(row -> row.getTableName() == ProductTableEnum.UNION_PRICE)
                .map(ProductSearchMap::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> manualProductIds = groupRows.stream()
                .filter(row -> row.getTableName() == ProductTableEnum.MANUAL_PRODUCT)
                .map(ProductSearchMap::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> sealedProductIds = groupRows.stream()
                .filter(row -> row.getTableName() == ProductTableEnum.SEALED_PRODUCT)
                .map(ProductSearchMap::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> supplyIds = groupRows.stream()
                .filter(row -> row.getTableName() == ProductTableEnum.SUPPLY)
                .map(ProductSearchMap::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, List<CardProduct>> offersByUnionPrice = new HashMap<>();
        Map<String, Long> searchMapIdByProductId = Map.of();
        Map<Long, UnionPrice> unionPriceById = new HashMap<>();
        if (!unionPriceIds.isEmpty()) {
            unionPriceById = unionPriceRepository.findAllById(unionPriceIds).stream()
                    .collect(Collectors.toMap(UnionPrice::getId, u -> u));
            List<CardProduct> allOffers = cardProductRepository.findVisibleWithUnionPriceByUnionPriceIds(unionPriceIds);
            offersByUnionPrice = allOffers.stream()
                    .collect(Collectors.groupingBy(cp -> cp.getUnionPrice().getId()));
            List<String> productIds = allOffers.stream()
                    .map(CardProduct::getPublicId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!productIds.isEmpty()) {
                searchMapIdByProductId = productSearchMapRepository
                        .findByTableNameAndProductIdIn(ProductTableEnum.CARD_PRODUCT, productIds)
                        .stream()
                        .collect(Collectors.toMap(ProductSearchMap::getProductId, ProductSearchMap::getId));
            }
        }

        Map<Long, ManualProduct> manualById = manualProductIds.isEmpty()
                ? Map.of()
                : manualProductRepository.findAllById(manualProductIds).stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()) && Boolean.TRUE.equals(m.getIsVisible()))
                        .collect(Collectors.toMap(ManualProduct::getId, m -> m, (a, b) -> a));

        Map<Long, SealedProduct> sealedById = sealedProductIds.isEmpty()
                ? Map.of()
                : sealedProductRepository.findAllById(sealedProductIds).stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()) && Boolean.TRUE.equals(s.getIsActive()))
                        .collect(Collectors.toMap(SealedProduct::getId, s -> s, (a, b) -> a));

        Map<Long, Supply> supplyById = supplyIds.isEmpty()
                ? Map.of()
                : supplyRepository.findAllById(supplyIds).stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()) && Boolean.TRUE.equals(s.getIsVisible()))
                        .collect(Collectors.toMap(Supply::getId, s -> s, (a, b) -> a));

        final Map<Long, UnionPrice> unionPriceByIdFinal = unionPriceById;
        List<ProductItemDto> ordered = new ArrayList<>();
        for (ProductSearchMap row : groupRows) {
            if (row.getTableName() == ProductTableEnum.UNION_PRICE) {
                List<CardProduct> offers = offersByUnionPrice.getOrDefault(row.getSourceId(), List.of());
                if (offers.isEmpty()) {
                    UnionPrice unionPrice = unionPriceByIdFinal.get(row.getSourceId());
                    if (unionPrice == null) {
                        continue;
                    }
                    ProductItemDto dto = productItemDtoMapper.fromUnionPriceOnly(unionPrice, row.getProductId());
                    if (dto != null) {
                        ordered.add(dto);
                    }
                    continue;
                }
                CardProduct any = offers.get(0);
                ProductItemDto dto = productItemDtoMapper.fromCardProducts(
                        any.getUnionPrice(), offers, row.getProductId(), searchMapIdByProductId);
                if (dto != null) {
                    ordered.add(dto);
                }
            } else if (row.getTableName() == ProductTableEnum.MANUAL_PRODUCT) {
                ManualProduct manual = manualById.get(row.getSourceId());
                if (manual == null) {
                    continue;
                }
                ProductItemDto dto = productItemDtoMapper.fromManualProduct(row.getId(), manual);
                if (dto != null) {
                    ordered.add(dto);
                }
            } else if (row.getTableName() == ProductTableEnum.SEALED_PRODUCT) {
                SealedProduct sealed = sealedById.get(row.getSourceId());
                if (sealed == null) {
                    continue;
                }
                ProductItemDto dto = productItemDtoMapper.fromSealedProduct(row.getId(), sealed);
                if (dto != null) {
                    ordered.add(dto);
                }
            } else if (row.getTableName() == ProductTableEnum.SUPPLY) {
                Supply supply = supplyById.get(row.getSourceId());
                if (supply == null) {
                    continue;
                }
                ProductItemDto dto = productItemDtoMapper.fromSupply(row.getId(), supply);
                if (dto != null) {
                    ordered.add(dto);
                }
            }
        }
        return new PageImpl<>(ordered, pageable, mapPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductItemDto getProductDetail(String id) {
        List<ProductSearchMap> productSearchMaps = productSearchMapRepository.findBySourcePublicId(id);
        if (productSearchMaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        ProductSearchMap productSearchMap = productSearchMaps.get(0);
        if (productSearchMap.getTableName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 정보를 찾을 수 없습니다.");
        }

        return switch (productSearchMap.getTableName()) {
            case UNION_PRICE -> getCardDetailByMap(productSearchMaps);
            case SEALED_PRODUCT -> getSealedProductDetailByMap(productSearchMaps);
            case MANUAL_PRODUCT -> getManualProductDetailByMap(productSearchMaps);
            case SUPPLY -> getSupplyProductDetailByMap(productSearchMaps);
            case CARD_PRODUCT, OTHER_PRODUCT -> throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "아직 구현되지 않은 기능입니다.");
        };
    }

    private ProductItemDto getCardDetailByMap(List<ProductSearchMap> productSearchMaps) {
        List<Long> unionPriceIds = productSearchMaps.stream()
                .map(ProductSearchMap::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (unionPriceIds.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 정보가 2개 이상 존재합니다.");
        }
        Long unionPriceId = unionPriceIds.get(0);

        String primaryProductId = productSearchMaps.stream()
                .map(ProductSearchMap::getProductId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        GameSalesInfoDto gameSalesInfo = gameSalesInfoService.getGameSalesInfo(productSearchMaps.get(0).getGame());

        List<CardProduct> offers = cardProductRepository.findVisibleWithUnionPriceByUnionPriceIds(List.of(unionPriceId));
        if (offers.isEmpty()) {
            UnionPrice unionPrice = unionPriceRepository.findById(unionPriceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card detail not found"));
            ProductItemDto fallback = productItemDtoMapper.fromUnionPriceOnly(unionPrice, primaryProductId, gameSalesInfo);
            if (fallback == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card detail not found");
            }
            return fallback;
        }

        List<String> productIds = offers.stream()
                .map(CardProduct::getPublicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, Long> searchMapIdByProductId = productIds.isEmpty()
                ? Map.of()
                : productSearchMapRepository.findByTableNameAndProductIdIn(ProductTableEnum.CARD_PRODUCT, productIds)
                        .stream()
                        .collect(Collectors.toMap(ProductSearchMap::getProductId, ProductSearchMap::getId));

        return productItemDtoMapper.fromCardProducts(
                offers.get(0).getUnionPrice(),
                offers,
                primaryProductId,
                searchMapIdByProductId,
                gameSalesInfo);
    }

    private ProductItemDto getManualProductDetailByMap(List<ProductSearchMap> productSearchMaps) {
        if (productSearchMaps.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 정보가 2개 이상 존재합니다.");
        }
        ProductSearchMap map = productSearchMaps.get(0);
        ManualProduct manualProduct = manualProductRepository.findById(map.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ManualProduct not found"));
        return productItemDtoMapper.fromManualProduct(map.getId(), manualProduct);
    }

    
    // 장바구니(Cart) 상품 조회
    private ProductItemDto getSealedProductDetailByMap(List<ProductSearchMap> productSearchMaps) {
        if (productSearchMaps.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sealed product detail not found");
        }
        ProductSearchMap map = productSearchMaps.get(0);
        SealedProduct sealedProduct = sealedProductRepository.findById(map.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SealedProduct not found"));
        if (Boolean.TRUE.equals(sealedProduct.getIsDeleted()) || !Boolean.TRUE.equals(sealedProduct.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sealed product not available");
        }
        return productItemDtoMapper.fromSealedProduct(map.getId(), sealedProduct);
    }

    private ProductItemDto getSupplyProductDetailByMap(List<ProductSearchMap> productSearchMaps) {
        if (productSearchMaps.size() != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 정보가 2개 이상 존재합니다.");
        }
        ProductSearchMap map = productSearchMaps.get(0);
        Supply supply = supplyRepository.findById(map.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supply not found"));
        if (Boolean.TRUE.equals(supply.getIsDeleted()) || !Boolean.TRUE.equals(supply.getIsVisible())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supply not available");
        }
        return productItemDtoMapper.fromSupply(map.getId(), supply);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductItemDto getProductItemDtoBySearchMapId(Long searchMapId) {
        ProductSearchMap map = productSearchMapRepository.findById(searchMapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (map.getTableName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품 정보를 찾을 수 없습니다.");
        }
        return switch (map.getTableName()) {
            case UNION_PRICE -> getProductItemDtoBySearchMapForUnionPrice(map);
            case CARD_PRODUCT -> getProductItemDtoBySearchMapForCardProduct(map);
            case SEALED_PRODUCT -> getProductItemDtoBySearchMapForSealedProduct(map);
            case MANUAL_PRODUCT -> getProductItemDtoBySearchMapForManualProduct(map);
            case SUPPLY -> getProductItemDtoBySearchMapForSupply(map);
            case OTHER_PRODUCT -> throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "아직 구현되지 않은 기능입니다.");
        };
    }

    private ProductItemDto getProductItemDtoBySearchMapForUnionPrice(ProductSearchMap productSearchMap) {
        UnionPrice unionPrice = unionPriceRepository.findById(productSearchMap.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UnionPrice not found"));
        List<CardProduct> offers = cardProductRepository.findVisibleWithUnionPriceByUnionPriceIds(List.of(unionPrice.getId()));
        if (offers.isEmpty()) {
            ProductItemDto fallback = productItemDtoMapper.fromUnionPriceOnly(unionPrice, productSearchMap.getProductId());
            if (fallback == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card detail not found");
            }
            return fallback;
        }

        List<String> productIds = offers.stream()
                .map(CardProduct::getPublicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, Long> searchMapIdByProductId = productIds.isEmpty()
                ? Map.of()
                : productSearchMapRepository.findByTableNameAndProductIdIn(ProductTableEnum.CARD_PRODUCT, productIds)
                        .stream()
                        .collect(Collectors.toMap(ProductSearchMap::getProductId, ProductSearchMap::getId));

        return productItemDtoMapper.fromCardProducts(
                unionPrice, offers, productSearchMap.getProductId(), searchMapIdByProductId);
    }

    private ProductItemDto getProductItemDtoBySearchMapForCardProduct(ProductSearchMap productSearchMap) {
        CardProduct cardProduct = cardProductRepository.findByIdWithUnionPrice(productSearchMap.getSourceId())
                .or(() -> cardProductRepository.findById(productSearchMap.getSourceId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CardProduct not found"));
        if (cardProduct.getUnionPrice() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UnionPrice not found");
        }
        List<CardProduct> offers = cardProductRepository
                .findVisibleWithUnionPriceByUnionPriceIds(List.of(cardProduct.getUnionPrice().getId()));
        if (offers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Card detail not found");
        }

        List<String> productIds = offers.stream()
                .map(CardProduct::getPublicId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, Long> searchMapIdByProductId = productIds.isEmpty()
                ? Map.of()
                : productSearchMapRepository.findByTableNameAndProductIdIn(ProductTableEnum.CARD_PRODUCT, productIds)
                        .stream()
                        .collect(Collectors.toMap(ProductSearchMap::getProductId, ProductSearchMap::getId));

        return productItemDtoMapper.fromCardProducts(
                cardProduct.getUnionPrice(), offers, cardProduct.getPublicId(), searchMapIdByProductId);
    }

    private ProductItemDto getProductItemDtoBySearchMapForSealedProduct(ProductSearchMap productSearchMap) {
        SealedProduct sealed = sealedProductRepository.findById(productSearchMap.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SealedProduct not found"));
        if (Boolean.TRUE.equals(sealed.getIsDeleted()) || !Boolean.TRUE.equals(sealed.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sealed product not available");
        }
        return productItemDtoMapper.fromSealedProduct(productSearchMap.getId(), sealed);
    }

    private ProductItemDto getProductItemDtoBySearchMapForManualProduct(ProductSearchMap productSearchMap) {
        ManualProduct manual = manualProductRepository.findById(productSearchMap.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ManualProduct not found"));
        if (Boolean.TRUE.equals(manual.getIsDeleted()) || !Boolean.TRUE.equals(manual.getIsVisible())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Manual product not available");
        }
        return productItemDtoMapper.fromManualProduct(productSearchMap.getId(), manual);
    }

    private ProductItemDto getProductItemDtoBySearchMapForSupply(ProductSearchMap productSearchMap) {
        Supply supply = supplyRepository.findById(productSearchMap.getSourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supply not found"));
        if (Boolean.TRUE.equals(supply.getIsDeleted()) || !Boolean.TRUE.equals(supply.getIsVisible())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Supply not available");
        }
        return productItemDtoMapper.fromSupply(productSearchMap.getId(), supply);
    }

}
