package com.shop.card.dto.slim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnionPriceSlimDto {

    private Long id;

    private String game;
    private String productType;
    private String printType;
    private String printing;
    private String setCode;
    private Long setNumber;
    private String cardName;
    private String cardNameK;
    private String imageSource;
    private String imageUrl;
    private String checkCodeRefined;
    private String rarity;
    private BigDecimal price;

}
