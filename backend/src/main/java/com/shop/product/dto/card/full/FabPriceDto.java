package com.shop.product.dto.card.full;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.card.entity.FabPrice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabPriceDto {

    // 판매 카드 가격 정보 Dto

    // 판매 카드 가격 ID
    private Long id;
    // 세트
    private String set;
    // 코드
    private String code;
    // 가격
    private BigDecimal price;
    // 레어도
    private String rarity;

    @JsonProperty("setname")
    // 세트 이름
    private String setName;
    // 카드 이름

    @JsonProperty("cardname")
    private String cardName;

    @JsonProperty("collector_num")
    private String collectorNum;

    private String foil;
    private Long tcgPPriceId;
    private String checkCode;
    private String conNumName;

    public FabPrice toEntity() {
        return FabPrice.builder()
                .id(id)
                .set(set)
                .code(code)
                .price(price)
                .rarity(rarity)
                .setName(setName)
                .cardName(cardName)
                .collectorNum(collectorNum)
                .foil(foil)
                .tcgPPriceId(tcgPPriceId)
                .checkCode(checkCode)
                .conNumName(conNumName)
                .build();
    }
}
