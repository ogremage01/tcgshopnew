package com.shop.product.dto.card.slim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shop.card.entity.MtgPrice;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MtgPriceSlimDto {

    // mtg 카드 가격 정보 Dto

    // mtg 카드 가격 ID
    private Long id;
    // 세트
    private String set;
    // 코드
    private String code;
    // 타입
    private String type;
    // 가격
    private BigDecimal price;

    // 이미지 주소
    private String imageUrl1;
    // 이미지 주소2
    private String imageUrl2;

    @JsonProperty("setname")
    private String setName;
    private String name;
    @JsonProperty("name_k")
    private String nameK;

    /**
     * mtg 카드 가격 정보 Dto를 mtg 카드 가격 엔티티로 변환
     * 
     * @return mtg 카드 가격 엔티티
     */
    public MtgPrice toEntity() {
        return MtgPrice.builder()
                .id(id)
                .set(set)
                .code(code)
                .type(type)
                .price(price)
                .setName(setName)
                .name(name)
                .nameK(nameK)
                .build();
    }
}
