package com.shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplySaleDto {

        // supplies 상품 타입
        private String suppliesType;

        // 테이블 내 상품 ID
        private Long tableId;
    
        // 제조사
        private String maker;
    
        // 상품 IP(MTG/FAB/해리포터 등등)
        private String productIp;
    
        // 상품 가격
        private Long price;
    
        // 표시 재고
        private Long currentVisibleStock;
}
