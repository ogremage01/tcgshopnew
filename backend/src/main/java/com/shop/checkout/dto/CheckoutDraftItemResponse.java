package com.shop.checkout.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutDraftItemResponse {
    private Long searchMapId;
    private String productType;
    private String productNameEn;
    private String productNameKo;
    private String imageUrl;
    /** 표시 우선순위는 클라이언트에서 영문 우선 */
    private String imageUrlEn;
    private String imageUrlKo;
    private BigDecimal snapshotUnitPrice;
    private Long quantity;
    private BigDecimal snapshotTotalPrice;
    private Long snapshotPointAmount;
}
