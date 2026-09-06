
package com.shop.product.service;

import java.math.BigDecimal;
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
                log.warn("Portfolio snapshot: linked selling-price formula is omitted.");
                if (product != null && !Boolean.TRUE.equals(product.getIsPriceLinked())) {
                        return product.getPrice();
                }
                return 0L;
        }

        @Override
        public Long calculatePriceFromUnionPrice(UnionPrice unionPrice) {
                log.warn("Portfolio snapshot: union selling-price formula is omitted.");
                return 0L;
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
                log.warn("Portfolio snapshot: USD conversion formula is omitted.");
                return null;
        }
}
