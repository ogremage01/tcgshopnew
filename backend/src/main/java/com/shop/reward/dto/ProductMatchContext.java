package com.shop.reward.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductMatchContext {
    private String game;
    private String productType;
    private String condition;
    private String language;
    private String set;
    private String rarity;
    private String printing;
    private String cardName;
    private String setNumber;
}
