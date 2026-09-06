package com.shop.scheduler.price.source.client;

import java.util.List;

import org.springframework.stereotype.Component;

import com.shop.scheduler.price.dto.FabSetInfoRawDto;
import com.shop.scheduler.price.dto.MtgSetInfoRawDto;
import com.shop.scheduler.price.dto.PriceRawDto;
import com.shop.scheduler.price.dto.PriceSyncDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 실제 외부 가격·세트 소스 URL·헤더·재시도는 포함하지 않습니다.
 */
@Slf4j
@Component
public class PriceFetchClient {
    public static final String PRICE_GUIDE_SOURCE = "external-price-source";

    public List<PriceSyncDto> fetchTcgPlayerPrices(Long setNameId, Long productTypeId) {
        logOmitted("TCG catalog price");
        return List.of();
    }

    public List<PriceRawDto> fetchMtgPriceRows() {
        logOmitted("MTG market price");
        return List.of();
    }

    public List<PriceRawDto> fetchFabPriceRows() {
        logOmitted("FAB market price");
        return List.of();
    }

    public List<MtgSetInfoRawDto> fetchMtgSetInfo() {
        logOmitted("MTG set info");
        return List.of();
    }

    public List<FabSetInfoRawDto> fetchFabSetInfo() {
        logOmitted("FAB set info");
        return List.of();
    }

    public void throttle() {
        // no-op in public snapshot
    }

    private static void logOmitted(String source) {
        log.warn("Portfolio snapshot: {} fetch is omitted.", source);
    }
}
