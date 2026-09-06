package com.shop.admin.product.dto.card;

import com.shop.product.dto.card.management.CardProductPatchCommand;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardProductPatchRequest {
    private Boolean isVisible;
    private Boolean isPriceLinked;
    private Long storageId;
    private Long currentVisibleStock;
    private Long maxVisibleStock;
    private Long totalStock;
    private Double pricingRate;
    private Long price;
    private String memo;

    public CardProductPatchCommand toCommand() {
        CardProductPatchCommand command = new CardProductPatchCommand();
        command.setIsVisible(isVisible);
        command.setIsPriceLinked(isPriceLinked);
        command.setStorageId(storageId);
        command.setCurrentVisibleStock(currentVisibleStock);
        command.setMaxVisibleStock(maxVisibleStock);
        command.setTotalStock(totalStock);
        command.setPricingRate(pricingRate);
        command.setPrice(price);
        command.setMemo(memo);
        return command;
    }
}
