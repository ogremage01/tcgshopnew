package com.shop.product.service.pricing;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PricingContext {

    /** game(GameEnum.game 풀네임) → 최소가격 */
    private final Map<String, BigDecimal> gameMinimumPrices;

    /** game(GameEnum.game 풀네임) → 환율 */
    private final Map<String, BigDecimal> gameCurrencyRates;

    private final Map<String, Double> gradePercentages;

    public BigDecimal minimumPrice(String game) {
        BigDecimal value = gameMinimumPrices.get(game);
        if (value == null) {
            throw new IllegalStateException("minimum_price config not found for game: " + game);
        }
        return value;
    }

    public BigDecimal currencyRate(String game) {
        BigDecimal value = gameCurrencyRates.get(game);
        if (value == null) {
            throw new IllegalStateException("us_currency_rate config not found for game: " + game);
        }
        return value;
    }

    public Double gradePercentage(String grade) {
        Double percentage = gradePercentages.get(grade);
        if (percentage == null) {
            throw new IllegalStateException("Grade pricing policy not found for grade: " + grade);
        }
        return percentage;
    }
}
