package com.shop.search.suggest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shop.search.dto.suggest.ProductSuggestDto;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MariaDbProductSuggestProvider implements ProductSuggestProvider {

    private static final int MAX_FETCH_MULTIPLIER = 3;
    private static final int MAX_FETCH_CAP = 60;

    private final ProductSearchMapRepository productSearchMapRepository;

    @Override
    public List<ProductSuggestDto> findByPrefix(String prefix, int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), 20);
        int fetchLimit = Math.min(cappedLimit * MAX_FETCH_MULTIPLIER, MAX_FETCH_CAP);
        String prefixLower = prefix.toLowerCase(Locale.ROOT);

        List<ProductSearchMap> maps = productSearchMapRepository.findByPrefixForSuggest(prefix, fetchLimit);
        Map<String, ProductSuggestDto> deduped = new LinkedHashMap<>();

        for (ProductSearchMap map : maps) {
            addIfMatches(deduped, map.getId(), map.getProductName(), prefixLower);
            addIfMatches(deduped, map.getId(), map.getProductNameKo(), prefixLower);
            if (deduped.size() >= cappedLimit) {
                break;
            }
        }

        List<ProductSuggestDto> results = new ArrayList<>(deduped.values());
        results.sort(Comparator.comparing(
                item -> item.getProductName().toLowerCase(Locale.ROOT)));
        if (results.size() > cappedLimit) {
            return results.subList(0, cappedLimit);
        }
        return results;
    }

    private void addIfMatches(
            Map<String, ProductSuggestDto> deduped,
            Long id,
            String name,
            String prefixLower) {
        if (name == null || name.isBlank() || id == null) {
            return;
        }
        if (!name.toLowerCase(Locale.ROOT).startsWith(prefixLower)) {
            return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        deduped.putIfAbsent(key, ProductSuggestDto.builder()
                .id(id)
                .productName(name)
                .build());
    }
}
