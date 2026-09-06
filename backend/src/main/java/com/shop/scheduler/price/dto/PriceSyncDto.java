package com.shop.scheduler.price.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.card.entity.TcgPPrice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceSyncDto {

    // TCGPlayer 가격 동기화 정보 Dto

    /** 외부 API는 productID(대문자 ID) 키를 사용한다. */
    @JsonProperty("productID")
    private Long productId;
    private String condition;
    private String game;
    private Boolean isSupplemental;
    private BigDecimal marketPrice;
    private String number;
    private String printing;
    private String productName;
    private String rarity;
    private String set;
    private String setAbbrv;
    private String type;

    /**
     * TCGPlayer 가격 엔티티로 변환한다.
     * 
     * @return TCGPlayer 가격 엔티티
     */
    public TcgPPrice toEntity() {
        return TcgPPrice.builder()
                .productId(productId)
                .condition(condition)
                .game(game)
                .isSupplemental(isSupplemental)
                .marketPrice(marketPrice)
                .number(number)
                .printing(printing)
                .productName(productName)
                .rarity(rarity)
                .set(set)
                .setAbbrv(setAbbrv)
                .type(type)
                .downloaded(false)
                .build();
    }
}
