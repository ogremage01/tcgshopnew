package com.shop.product.dto.card;

import com.shop.product.entity.card.CardProductLanguage;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardProductLanguageDto {

    private String code;
    private String displayName;
    private String displayNameKo;

    public static CardProductLanguageDto from(CardProductLanguage language) {
        return CardProductLanguageDto.builder()
                .code(language.getCode())
                .displayName(language.getDisplayName())
                .displayNameKo(language.getDisplayNameKo())
                .build();
    }
}
