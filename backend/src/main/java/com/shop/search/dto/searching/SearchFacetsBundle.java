package com.shop.search.dto.searching;

import java.io.Serializable;
import java.util.List;

/**
 * searchInit에서 facet 전용으로 조회하는 묶음. Redis 등 캐시에 넣기 쉽도록 단순한 구조로 둔다.
 */
public record SearchFacetsBundle(
        List<String> games,
        List<String> productTypes,
        List<SuppliesTypeFacetDto> suppliesTypes,
        List<String> manualCategories,
        List<String> printTypes,
        List<String> rarities,
        List<String> setNames)
        implements Serializable {
}
