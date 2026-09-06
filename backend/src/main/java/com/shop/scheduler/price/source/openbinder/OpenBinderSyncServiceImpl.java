package com.shop.scheduler.price.source.openbinder;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 외부 마켓 가격 동기화 파이프라인은 포함하지 않습니다.
 */
@Slf4j
@Service
public class OpenBinderSyncServiceImpl implements OpenBinderSyncService {

    private static final String OMITTED = "failure:omitted in public snapshot";

    @Override
    public OpenBinderSyncResult syncAll() {
        log.warn("Portfolio snapshot: market price sync is omitted.");
        return new OpenBinderSyncResult("failure", OMITTED);
    }

    @Override
    public String syncMtgPrices() {
        return OMITTED;
    }

    @Override
    public String syncFabPrices() {
        return OMITTED;
    }

    @Override
    public String syncMtgSetInfo() {
        return OMITTED;
    }

    @Override
    public String syncFabSetInfo() {
        return OMITTED;
    }
}
