package com.shop.product.service.pricing;

import java.util.List;
import java.util.Optional;

import com.shop.product.dto.card.management.PriceConfigDto;

public interface PriceConfigService {

    /**
     * 특정 게임의 가격 설정을 조회한다.
     *
     * @param configKey  설정 키
     * @param configGame GameEnum.game 풀네임 (예: "Magic: The Gathering")
     */
    Optional<PriceConfigDto> getPriceConfig(String configKey, String configGame);

    /**
     * 특정 키의 모든 게임 설정 목록을 조회한다 (부트스트랩용).
     *
     * @param configKey 설정 키
     */
    List<PriceConfigDto> getPriceConfigsByKey(String configKey);

    /**
     * 가격 설정을 저장한다. PriceConfigDto.configGame 이 필수다.
     *
     * @param priceConfigDto 설정 값
     */
    void setPriceConfig(PriceConfigDto priceConfigDto);
}
