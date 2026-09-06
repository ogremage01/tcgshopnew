package com.shop.scheduler.core.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shop.scheduler.metadata.application.MetadataService;
import com.shop.scheduler.price.application.PriceService;
import com.shop.scheduler.price.application.selling.CardSellingPriceUpdateService;
import com.shop.scheduler.price.service.union.UnionPriceIngestionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 카드데이터 업데이트 스케줄

@Slf4j
@Component
@RequiredArgsConstructor
public class CardDataUpdateScheduler {

    private final MetadataService metadataService;
    private final PriceService priceService;
    private final UnionPriceIngestionService unionPriceIngestionService;
    private final CardSellingPriceUpdateService cardSellingPriceUpdateService;
    
    @Scheduled(cron = "0 0 14 * * *")
    public void syncOpenBinderPricesScheduled() {
        String result = priceService.syncOpenBinderPricesWithPriority();
        if ("failure".equalsIgnoreCase(result)) {
            log.warn("14:00 Open Binder sync failed. Skip card selling price update.");
            return;
        }
        waitWhile(priceService::isOpenBinderSyncRunning, "14:00 Open Binder sync completion");
        cardSellingPriceUpdateService.startUpdateCardCalculatedLinkedPrice();
    }

    //시간 세팅 제대로 되었는지 확인
    @Scheduled(cron = "0 0 2 * * *")
    public void updateCardDataScheduled() {
        if (!metadataService.syncAllWithPriority()) {
            log.warn("Card data update did not start/complete on scheduler-priority path.");
            return;
        }

        if (!priceService.startSyncPricesWithPriority()) {
            log.warn("Card data update did not start on scheduler-priority path.");
            return;
        }

        waitWhile(priceService::isSyncRunning, "card data update completion");

        String openBinderResult = priceService.syncOpenBinderPricesWithPriority();
        if ("failure".equalsIgnoreCase(openBinderResult)) {
            log.warn("Open Binder sync failed. Skip remaining card data update steps.");
            return;
        }
        waitWhile(priceService::isOpenBinderSyncRunning, "Open Binder sync completion");

        int backfilled = unionPriceIngestionService.backfillMissingPublicIds();
        if (backfilled > 0) {
            log.info("Backfilled missing union public ids. count={}", backfilled);
        }
        cardSellingPriceUpdateService.startUpdateCardCalculatedLinkedPrice();
    }

    private void waitWhile(BooleanSupplier stillRunning, String waitContext) {
        long nanos = TimeUnit.SECONDS.toNanos(1);
        while (stillRunning.getAsBoolean()) {
            LockSupport.parkNanos(nanos);
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Interrupted while waiting for {}.", waitContext);
                return;
            }
        }
    }
}
