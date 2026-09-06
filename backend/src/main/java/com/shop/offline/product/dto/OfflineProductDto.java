package com.shop.offline.product.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineProductDto {

    private Long id;
    private String productId;
    private String categoryId;
    private String categoryTitle;
    private String title;
    private Integer priceUnit;
    private Integer priceValue;
    private String barcode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String linkTableName;
    private Long linkId;
    private Integer receivingQuantity;
    private Integer shippingQuantity;
    /** receivingQuantity - shippingQuantity (영속 필드 아님) */
    private Integer stockQuantity;
}
