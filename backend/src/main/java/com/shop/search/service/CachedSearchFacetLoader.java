package com.shop.search.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.shop.card.repository.UnionPriceRepository;
import com.shop.config.SearchFacetExecutorConfig;
import com.shop.product.service.supply.SuppliesTypeLabelResolver;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.search.dto.searching.ProductSearchingDto;
import com.shop.search.dto.searching.SearchFacetsBundle;
import com.shop.search.repository.map.ProductSearchMapRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 검색 facet 조회를 병렬 실행하며, 옵션(Redis)으로 결과를 캐시한다.
 */
@Slf4j
@Component
public class CachedSearchFacetLoader {

    private final ProductSearchMapRepository productSearchMapRepository;
    private final UnionPriceRepository unionPriceRepository;
    private final SealedProductRepository sealedProductRepository;
    private final SuppliesTypeLabelResolver suppliesTypeLabelResolver;
    private final Executor searchFacetExecutor;

    public CachedSearchFacetLoader(
            ProductSearchMapRepository productSearchMapRepository,
            UnionPriceRepository unionPriceRepository,
            SealedProductRepository sealedProductRepository,
            SuppliesTypeLabelResolver suppliesTypeLabelResolver,
            @Qualifier(SearchFacetExecutorConfig.SEARCH_FACET_EXECUTOR) Executor searchFacetExecutor) {
        this.productSearchMapRepository = productSearchMapRepository;
        this.unionPriceRepository = unionPriceRepository;
        this.sealedProductRepository = sealedProductRepository;
        this.suppliesTypeLabelResolver = suppliesTypeLabelResolver;
        this.searchFacetExecutor = searchFacetExecutor;
    }

    /**
     * 동일 필터 조건에서 facet 쿼리 반복을 줄이기 위한 캐시(선택). TTL은 {@code CacheManager} 설정을 따른다.
     */
    @Cacheable(
            cacheNames = "searchFacets",
            keyGenerator = "searchFacetsCacheKeyGenerator",
            unless = "#result == null")
    public SearchFacetsBundle loadFacetsParallel(ProductSearchingDto query) {
        long t0 = System.nanoTime();
        CompletableFuture<List<String>> games = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctGamesForSearch(query), searchFacetExecutor);
        CompletableFuture<List<String>> productTypes = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctProductTypesForSearch(query), searchFacetExecutor);
        CompletableFuture<List<String>> suppliesTypes = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctSuppliesTypesForSearch(query), searchFacetExecutor);
        CompletableFuture<List<String>> manualCategories = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctManualCategoriesForSearch(query), searchFacetExecutor);
        CompletableFuture<List<String>> printTypes = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctPrintTypesForSearch(query), searchFacetExecutor);
        CompletableFuture<List<Long>> unionPriceIdsFuture = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctUnionPriceSourceIdsForSearch(query), searchFacetExecutor);
        CompletableFuture<List<Long>> sealedProductIdsFuture = CompletableFuture.supplyAsync(
                () -> productSearchMapRepository.findDistinctSealedProductSourceIdsForSearch(query), searchFacetExecutor);

        try {
            CompletableFuture.allOf(
                            games,
                            productTypes,
                            suppliesTypes,
                            manualCategories,
                            printTypes,
                            unionPriceIdsFuture,
                            sealedProductIdsFuture)
                    .join();
            List<Long> unionPriceIds = unionPriceIdsFuture.join();
            List<Long> sealedProductIds = sealedProductIdsFuture.join();

            CompletableFuture<List<String>> raritiesFuture = unionPriceIds.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : CompletableFuture.supplyAsync(
                            () -> unionPriceRepository.findDistinctRaritiesByIds(unionPriceIds), searchFacetExecutor);
            CompletableFuture<List<String>> unionSetNamesFuture = unionPriceIds.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : CompletableFuture.supplyAsync(
                            () -> unionPriceRepository.findDistinctSetNamesByIds(unionPriceIds), searchFacetExecutor);
            CompletableFuture<List<String>> sealedSetNamesFuture = sealedProductIds.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : CompletableFuture.supplyAsync(
                            () -> sealedProductRepository.findDistinctSetNamesByIds(sealedProductIds),
                            searchFacetExecutor);

            CompletableFuture.allOf(raritiesFuture, unionSetNamesFuture, sealedSetNamesFuture).join();

            List<String> mergedSetNames = Stream.concat(
                            unionSetNamesFuture.join().stream(), sealedSetNamesFuture.join().stream())
                    .filter(Objects::nonNull)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .sorted()
                    .toList();

            SearchFacetsBundle bundle = new SearchFacetsBundle(
                    games.join(),
                    productTypes.join(),
                    suppliesTypeLabelResolver.resolveFacets(suppliesTypes.join()),
                    manualCategories.join(),
                    printTypes.join(),
                    raritiesFuture.join().stream()
                            .filter(Objects::nonNull)
                            .filter(s -> !s.isBlank())
                            .toList(),
                    mergedSetNames);
            if (log.isDebugEnabled()) {
                log.debug(
                        "search perf: facetsParallel+cacheable took {} ms",
                        (System.nanoTime() - t0) / 1_000_000);
            }
            return bundle;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
                cause = e;
            }
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("facet load failed", cause);
        }
    }
}
