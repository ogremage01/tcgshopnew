package com.shop.scheduler.stock.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shop.scheduler.stock.application.StockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockScheduler {

    private static final String STOCK_LOG_SOURCE = "scheduler.stock";

    private final StockService stockService;

    @Scheduled(cron = "0 0 14 * * *")
    public void syncStockScheduled() {
        if (!stockService.startSyncStock(STOCK_LOG_SOURCE)) {
            log.warn("Stock sync is already running. Skip scheduled run.");
        }
    }
}
