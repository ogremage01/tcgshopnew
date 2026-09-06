package com.shop.search.service;

import com.shop.search.dto.suggest.ProductSuggestResponseDto;

public interface ProductSuggestService {

    ProductSuggestResponseDto suggest(String query, int limit);
}
