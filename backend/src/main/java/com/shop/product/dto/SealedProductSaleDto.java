package com.shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SealedProductSaleDto {

    // 밀봉 제품 판매 정보
    private Long id;
    // 검색 매핑 ID(ProductSearchMap.ID) - 장바구니 식별용
    private Long searchMapId;
    // 밀봉 제품 타입
    private String language;
    // 표시 재고
    private Long currentVisibleStock;
    // 표시 가격
    private Long showingPrice;
}
