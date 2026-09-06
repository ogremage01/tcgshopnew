package com.shop.scheduler.price.source.openbinder;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 마켓가 오버레이 SQL은 포함하지 않습니다.
 */
@Slf4j
@Service
public class OpenBinderMarketPriceOverlayServiceImpl implements OpenBinderMarketPriceOverlayService {

    @Override
    public int applyFabMarketPricesToTcgP() {
        log.warn("Portfolio snapshot: market price overlay is omitted.");
        return 0;
    }

    @Override
    public int applyMtgMarketPricesToTcgP() {
        log.warn("Portfolio snapshot: market price overlay is omitted.");
        return 0;
    }
}
