package com.shop.search.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.search.dto.searching.ProductSearchingDto;
import com.shop.search.entity.ProductSearchMap;

public interface ProductSearchMapRepositoryCustom {

    Page<ProductSearchMap> findByProductSearchingDto(ProductSearchingDto query, Pageable pageable);

    /**
     * 현재 검색 조건과 동일한 WHERE로 보이는 행에서 distinct game (정렬 오름차순).
     */
    List<String> findDistinctGamesForSearch(ProductSearchingDto query);

    List<String> findDistinctProductTypesForSearch(ProductSearchingDto query);

    List<String> findDistinctSuppliesTypesForSearch(ProductSearchingDto query);

    List<String> findDistinctManualCategoriesForSearch(ProductSearchingDto query);

    List<String> findDistinctPrintTypesForSearch(ProductSearchingDto query);

    /**
     * UNION_PRICE 행의 sourceId(= UnionPrice.id) 목록. 레어·세트명 facet용.
     */
    List<Long> findDistinctUnionPriceSourceIdsForSearch(ProductSearchingDto query);

    /**
     * SEALED_PRODUCT 행의 sourceId(= SealedProduct.id) 목록. 세트명 facet용.
     */
    List<Long> findDistinctSealedProductSourceIdsForSearch(ProductSearchingDto query);

    /**
     * 자동완성용 전방일치 조회. isVisible=true, CARD_PRODUCT 제외.
     */
    List<ProductSearchMap> findByPrefixForSuggest(String prefix, int limit);

}
