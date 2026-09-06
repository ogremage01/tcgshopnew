package com.shop.order.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderProductDto {

    // 주문 상품 정보 Dto

    private Long id;
    private Long orderInfoId;
    private Long productId;
    private Long quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private String imageUrl;
    private String productNameEn;
    private String productNameKo;
    private Long rewardPoints;
}
