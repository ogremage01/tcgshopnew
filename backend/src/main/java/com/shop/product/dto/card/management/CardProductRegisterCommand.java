package com.shop.product.dto.card.management;

import com.shop.product.entity.card.CardProduct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductRegisterCommand {

    private String productType;
    private String cardName;
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
    private Long unionPriceId;

    public CardProduct toEntity() {
        return CardProduct.builder()
                .id(null)
                .productType(productType)
                .condition(condition)
                .printType(printType)
                .language(language)
                .isVisible(isVisible)
                .isDeleted(Boolean.TRUE.equals(isDeleted))
                .currentVisibleStock(currentVisibleStock)
                .maxVisibleStock(maxVisibleStock)
                .totalStock(totalStock)
                .isAutoUpdatedStock(isAutoUpdatedStock)
                .storageId(storageId)
                .isPriceLinked(isPriceLinked)
                .pricingRate(pricingRate)
                .price(price)
                .memo(memo)
                .build();
    }
}
