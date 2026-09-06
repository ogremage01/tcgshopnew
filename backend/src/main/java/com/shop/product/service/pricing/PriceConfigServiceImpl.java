package com.shop.product.service.pricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.shop.product.dto.card.management.PriceConfigDto;
import com.shop.product.entity.card.policy.PriceConfig;
import com.shop.product.entity.card.policy.PriceConfigId;
import com.shop.product.enums.GameEnum;
import com.shop.product.repository.policy.PriceConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.shop.scheduler.price.application.selling.CardSellingPriceUpdateService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceConfigServiceImpl implements PriceConfigService {

    private static final String CONFIG_KEY_MINIMUM_PRICE = "minimum_price";
    private static final String CONFIG_KEY_US_CURRENCY_RATE = "us_currency_rate";

    private final PriceConfigRepository priceConfigRepository;
    private final CardSellingPriceUpdateService cardSellingPriceUpdateService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public Optional<PriceConfigDto> getPriceConfig(String configKey, String configGame) {
        Optional<PriceConfigDto> exact = priceConfigRepository
                .findByConfigKeyAndConfigGame(configKey, configGame)
                .map(this::toDto);
        if (exact.isPresent() || configGame == null) {
            return exact;
        }
        String canonicalGame = GameEnum.toConfigGame(configGame);
        if (canonicalGame.equals(configGame)) {
            return exact;
        }
        return priceConfigRepository.findByConfigKeyAndConfigGame(configKey, canonicalGame)
                .map(this::toDto);
    }

    @Override
    public List<PriceConfigDto> getPriceConfigsByKey(String configKey) {
        return priceConfigRepository.findByConfigKey(configKey).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void setPriceConfig(PriceConfigDto priceConfigDto) {
        String configGame = priceConfigDto.getConfigGame() != null ? priceConfigDto.getConfigGame() : "";

        Long lastMinimumPrice = transactionTemplate.execute(status -> {
            PriceConfig existingConfig = priceConfigRepository
                    .findByConfigKeyAndConfigGame(priceConfigDto.getConfigKey(), configGame)
                    .orElse(null);

            Long previousMinimumPrice = null;
            if (existingConfig != null && CONFIG_KEY_MINIMUM_PRICE.equals(existingConfig.getConfigKey())) {
                previousMinimumPrice = existingConfig.getConfigValue().longValue();
            }

            PriceConfig priceConfig = PriceConfig.builder()
                    .configKey(priceConfigDto.getConfigKey())
                    .configGame(configGame)
                    .configValue(priceConfigDto.getConfigValue())
                    .build();

            priceConfigRepository.save(priceConfig);
            return previousMinimumPrice;
        });

        if (CONFIG_KEY_MINIMUM_PRICE.equals(priceConfigDto.getConfigKey()) && lastMinimumPrice != null) {
            Long newMinimumPrice = priceConfigDto.getConfigValue().longValue();
            if (lastMinimumPrice < newMinimumPrice) {
                cardSellingPriceUpdateService.raiseMinimumPriceForCardProducts(newMinimumPrice, configGame);
            } else {
                cardSellingPriceUpdateService.dropMinimumPriceForCardProducts(newMinimumPrice, lastMinimumPrice, configGame);
            }
        }

        if (CONFIG_KEY_US_CURRENCY_RATE.equals(priceConfigDto.getConfigKey())) {
            cardSellingPriceUpdateService.startUpdateCardCalculatedLinkedPrice();
        }
    }

    private PriceConfigDto toDto(PriceConfig entity) {
        return PriceConfigDto.builder()
                .configKey(entity.getConfigKey())
                .configGame(entity.getConfigGame())
                .configValue(entity.getConfigValue())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
