package com.shop.scheduler.price.service.link.deprecated;

import lombok.extern.slf4j.Slf4j;

/**
 * [DEPRECATED] 레거시 가격 링크. 공개본에서는 매칭 구현을 포함하지 않습니다.
 */
@Deprecated
@Slf4j
public class PriceLinkServiceLegacy {

    @Deprecated
    public void syncMtgPriceWithTcgPrice() {
        log.warn("Portfolio snapshot: legacy price-link matching is omitted.");
    }

    @Deprecated
    public void syncFabPriceWithTcgPrice() {
        log.warn("Portfolio snapshot: legacy price-link matching is omitted.");
    }
}
