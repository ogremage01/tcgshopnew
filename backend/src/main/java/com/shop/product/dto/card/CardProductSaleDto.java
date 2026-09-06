package com.shop.product.dto.card;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductSaleDto {

    // 카드 판매 요청 정보 Dto

    // 등록 ID(CardProduct.ID)
    private Long id;
    // 검색 매핑 ID(ProductSearchMap.ID) - 장바구니 식별용
    private Long searchMapId;
    // 카드 상태
    private String condition;
    // 표시 재고
    private Long currentVisibleStock;
    // 표시 가격
    private Long showingPrice;
    // 표시 가격-달러
    private BigDecimal showingPriceUsd;

}
