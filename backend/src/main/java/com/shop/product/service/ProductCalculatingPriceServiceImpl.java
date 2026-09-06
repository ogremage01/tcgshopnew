
package com.shop.product.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.shop.card.entity.UnionPrice;
import com.shop.card.service.TcgPPriceService;
import com.shop.product.dto.card.CardProductSaleDto;
import com.shop.product.dto.card.slim.TcgPPriceCardSlimDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.card.policy.GradePricingPolicy;
import com.shop.product.entity.card.policy.PriceConfig;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.policy.GradePricingPolicyRepository;
import com.shop.product.repository.policy.PriceConfigRepository;
import com.shop.product.service.pricing.PricingContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCalculatingPriceServiceImpl implements ProductCalculatingPriceService {

        private static final String CONFIG_KEY_MINIMUM_PRICE = "minimum_price";
        private static final String CONFIG_KEY_US_CURRENCY_RATE = "us_currency_rate";
        private static final String GRADE_NM = "NM";

        private final TcgPPriceService tcgPPriceService;
        private final PriceConfigRepository priceConfigRepository;
        private final GradePricingPolicyRepository gradePricingPolicyRepository;
        private final CardProductRepository cardProductRepository;

        @Override
        public PricingContext loadPricingContext() {
                Map<String, BigDecimal> gameMinimumPrices = priceConfigRepository
                                .findByConfigKey(CONFIG_KEY_MINIMUM_PRICE).stream()
                                .collect(Collectors.toMap(PriceConfig::getConfigGame, PriceConfig::getConfigValue));

                Map<String, BigDecimal> gameCurrencyRates = priceConfigRepository
                                .findByConfigKey(CONFIG_KEY_US_CURRENCY_RATE).stream()
                                .collect(Collectors.toMap(PriceConfig::getConfigGame, PriceConfig::getConfigValue));

                Map<String, Double> gradePercentages = gradePricingPolicyRepository.findAll().stream()
                                .collect(Collectors.toMap(
                                                GradePricingPolicy::getGrade,
                                                GradePricingPolicy::getPercentage,
                                                (a, b) -> a));

                return PricingContext.builder()
                                .gameMinimumPrices(gameMinimumPrices)
                                .gameCurrencyRates(gameCurrencyRates)
                                .gradePercentages(gradePercentages)
                                .build();
        }

        @Override
        public Long calculateProductPrice(CardProduct product, BigDecimal marketPrice) {
                return calculateProductPrice(product, marketPrice, loadPricingContext());
        }

        @Override
        public Long calculateProductPrice(CardProduct product, BigDecimal marketPrice, PricingContext context) {
                if (!product.getIsPriceLinked()) {
                        return product.getPrice();
                }
                String game = resolveGame(product);
                Double rate = product.getPricingRate();
                BigDecimal pricingRate = rate != null ? BigDecimal.valueOf(rate) : BigDecimal.ONE;
                return calculateLinkedPrice(marketPrice, pricingRate, product.getCondition(), context, game);
        }

        @Override
        public Long calculatePriceFromUnionPrice(UnionPrice unionPrice) {
                if (unionPrice == null || unionPrice.getPrice() == null) {
                        return 0L;
                }
                PricingContext context = loadPricingContext();
                return calculateLinkedPrice(unionPrice.getPrice(), BigDecimal.ONE, GRADE_NM, context, unionPrice.getGame());
        }

        private Long calculateLinkedPrice(BigDecimal marketPrice, BigDecimal pricingRate, String grade, PricingContext context, String game) {
                Double gradePrice = context.gradePercentage(grade);
                BigDecimal rate = context.currencyRate(game);
                BigDecimal calculated = marketPrice
                                .multiply(pricingRate)
                                .multiply(BigDecimal.valueOf(gradePrice))
                                .multiply(rate);

                BigDecimal rounded = calculated
                                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING)
                                .multiply(BigDecimal.valueOf(100));

                return rounded.max(context.minimumPrice(game)).longValue();
        }

        @Override
        public Page<CardProductSaleDto> findByProductName(String keyword, Pageable pageable) {

                List<TcgPPriceCardSlimDto> tcgPPriceCardSlimDtos = tcgPPriceService
                                .findByProductNameOrCodeNumberList(keyword);

                Page<CardProduct> cardProducts = cardProductRepository.findByProductIdInVisibleAndIsNotDeleted(
                                tcgPPriceCardSlimDtos.stream().map(TcgPPriceCardSlimDto::getId)
                                                .collect(Collectors.toList()),
                                pageable);

                return new PageImpl<>(null, pageable, cardProducts.getTotalElements());
        }

        @Override
        public BigDecimal calculateProductPriceUsd(Long krwPrice, String game) {
                if (game == null || krwPrice == null) {
                        return null;
                }
                PricingContext context = loadPricingContext();
                BigDecimal rate = context.getGameCurrencyRates().get(game);
                if (rate == null) {
                        log.warn("us_currency_rate not found for game: {}. USD price will be null.", game);
                        return null;
                }
                return BigDecimal.valueOf(krwPrice).divide(rate, 2, RoundingMode.HALF_UP);
        }

        private String resolveGame(CardProduct product) {
                UnionPrice unionPrice = product.getUnionPrice();
                if (unionPrice == null || unionPrice.getGame() == null) {
                        throw new IllegalStateException(
                                "Cannot determine game for CardProduct id=" + product.getId()
                                        + ": unionPrice or game is null");
                }
                return unionPrice.getGame();
        }
}
