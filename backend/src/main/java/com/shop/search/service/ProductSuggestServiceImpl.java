package com.shop.search.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.search.dto.suggest.ProductSuggestResponseDto;
import com.shop.search.suggest.ProductSuggestProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductSuggestServiceImpl implements ProductSuggestService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MIN_QUERY_LENGTH = 2;

    private final ProductSuggestProvider productSuggestProvider;

    @Override
    @Transactional(readOnly = true)
    public ProductSuggestResponseDto suggest(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query is required");
        }
        String trimmed = query.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Query must be at least " + MIN_QUERY_LENGTH + " characters");
        }

        int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        return ProductSuggestResponseDto.builder()
                .items(productSuggestProvider.findByPrefix(trimmed, resolvedLimit))
                .build();
    }
}
