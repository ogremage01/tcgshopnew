package com.shop.config;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import com.shop.search.dto.searching.ProductSearchingDto;

@Component("searchFacetsCacheKeyGenerator")
public class SearchFacetsCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (params.length < 1 || !(params[0] instanceof ProductSearchingDto q)) {
            return new FacetsKey("invalid", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "",
                    "DEFAULT", "UPDATED", null, null);
        }
        return new FacetsKey(
                Objects.toString(q.getKeyword(), ""),
                sortKey(q.getGames()),
                sortKey(q.getProductTypes()),
                sortKey(q.getSuppliesTypes()),
                sortKey(q.getManualCategories()),
                sortKey(q.getRarities()),
                sortKey(q.getSetNames()),
                Objects.toString(q.getSetCode(), ""),
                String.valueOf(q.getResolvedSearchMode()),
                String.valueOf(q.getResolvedEntryState()),
                q.getIsFoil(),
                q.getIsInStock());
    }

    private static List<String> sortKey(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        ArrayList<String> copy = new ArrayList<>(list);
        Collections.sort(copy);
        return List.copyOf(copy);
    }

    private record FacetsKey(
            String keyword,
            List<String> games,
            List<String> productTypes,
            List<String> suppliesTypes,
            List<String> manualCategories,
            List<String> rarities,
            List<String> setNames,
            String setCode,
            String searchMode,
            String entryState,
            Boolean isFoil,
            Boolean isInStock)
            implements Serializable {
    }
}
