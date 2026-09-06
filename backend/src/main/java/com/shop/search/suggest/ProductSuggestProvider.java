package com.shop.search.suggest;

import java.util.List;

import com.shop.search.dto.suggest.ProductSuggestDto;

public interface ProductSuggestProvider {

    List<ProductSuggestDto> findByPrefix(String prefix, int limit);
}
