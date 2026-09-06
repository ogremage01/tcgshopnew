package com.shop.product.dto.card.management;

import com.querydsl.core.annotations.QueryProjection;
import com.shop.card.entity.UnionPrice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@QueryProjection
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductManagementDto {

    private Long id;
    private String name;
    private String imageUrl;
    private UnionPrice unionPrice;
    private String condition;
    private String printType;
    private String language;
    private Boolean isVisible;
    private Boolean isDeleted;
    private Long currentVisibleStock;
    private Long maxVisibleStock;
    private Long totalStock;
    private Boolean isAutoUpdatedStock;
    private Long storageId;
    private Boolean isPriceLinked;
    private Double pricingRate;
    private Long price;
    private String memo;
}
