package com.shop.product.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.card.entity.UnionPrice;
import com.shop.product.dto.card.CardProductSaleDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.service.pricing.PricingContext;

public interface ProductCalculatingPriceService {

    // 판매 상품 서비스

    Page<CardProductSaleDto> findByProductName(String keyword, Pageable pageable);

    PricingContext loadPricingContext();

    Long calculateProductPrice(CardProduct product, BigDecimal marketPrice);

    Long calculateProductPrice(CardProduct product, BigDecimal marketPrice, PricingContext context);

    /** CardProduct 없이 UnionPrice만으로 NM 등급 + 환율을 적용한 기준가를 계산한다. */
    Long calculatePriceFromUnionPrice(UnionPrice unionPrice);

    /**
     * 한화 가격을 게임별 환율로 나눠 달러 가격을 계산한다.
     *
     * @param game GameEnum.game 풀네임 (예: "Magic: The Gathering"). null 이면 null 반환.
     */
    BigDecimal calculateProductPriceUsd(Long krwPrice, String game);
}
