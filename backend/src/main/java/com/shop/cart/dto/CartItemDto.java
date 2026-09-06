package com.shop.cart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long id;
    private Long searchMapId;
    private String productType;
    private Long currentVisibleStock;
    private Long price;
    /** 표시 가격(USD). EN 로케일 표시용 */
    private BigDecimal priceUsd;
    private Long quantity;
    /** 카트 행 수정 시각(주문 확정 시 일치 검증용). 없으면 null */
    private LocalDateTime cartItemUpdatedAt;
    private String imageUrl;
    private String productNameEn;
    private String productNameKo;
    private CardProductDto cardProduct;
    private SuppliesProductDto suppliesProduct;
    private SealedProductDto sealedProduct;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardProductDto {
        private String game;
        private String condition;
        private String language;
        private String printType;
        private String printing;
        private String setCode;
        private String setNumber;
        private String setName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuppliesProductDto {
        private String suppliesType;
        private String table;
        private String tableId;
        private String maker;
        private String productIp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SealedProductDto {
        private String game;
        private String language;
    }
}