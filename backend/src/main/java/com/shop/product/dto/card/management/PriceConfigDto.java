package com.shop.product.dto.card.management;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceConfigDto {

    /**
     * 
     * @example "minimum_price"
     * @example "us_currency_rate"
     */
    private String configKey;
    /**
     * 
     * @example "Magic: The Gathering"
     * @example "Flesh & Blood TCG"
     * @example "Star Wars Unlimited"
     * @example "Riftbound League of Legends Trading Card Game"
     */
    // GameEnum.game 풀네임 (예: "Magic: The Gathering"). 빈 문자열은 전역값.
    private String configGame;
    private BigDecimal configValue;
    private LocalDateTime updatedAt;

}
