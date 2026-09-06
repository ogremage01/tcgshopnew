package com.shop.product.dto.card.full;

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
public class MtgPriceDto {

    // 매진 카드 가격 정보 Dto

    // 매진 카드 가격 ID
    private Long id;
    // 세트
    private String set;
    // 코드
    private String code;
    // 타입
    private String type;
    // 가격
    private BigDecimal price;
    // TCGPrice ID
    private Long tcgPPriceId;

    @JsonProperty("setname")
    private String setName;
    private String name;
    @JsonProperty("name_k")
    private String nameK;

    private String checkCode;
    private String checkCodeRefined;
    private String conNumName;

    /**
     * 매직 더 개더링 카드 가격 정보 Dto를 매진 카드 가격 엔티티로 변환
     * 
     * @return 매직 더 개더링 카드 가격 엔티티
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
                .tcgPPriceId(tcgPPriceId)
                .checkCode(checkCode)
                .checkCodeRefined(checkCodeRefined)
                .conNumName(conNumName)
                .build();
    }
}
