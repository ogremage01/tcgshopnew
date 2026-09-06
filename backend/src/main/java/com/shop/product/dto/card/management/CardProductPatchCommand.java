package com.shop.product.dto.card.management;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardProductPatchCommand {
    private Boolean isVisible;
    private Boolean isPriceLinked;
    private Long storageId;
    private Long currentVisibleStock;
    private Long maxVisibleStock;
    private Long totalStock;
    private Double pricingRate;
    private Long price;
    private String memo;
}
