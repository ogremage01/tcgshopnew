package com.shop.product.repository.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.product.dto.card.management.SearchBySetCriteriaDto;
import com.shop.product.entity.card.CardProduct;

public interface CardProductRepositoryCustom {

    Page<CardProduct> findBySetForAdmin(SearchBySetCriteriaDto criteria, Pageable pageable);

}
