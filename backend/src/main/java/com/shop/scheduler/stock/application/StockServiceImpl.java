package com.shop.scheduler.stock.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.log.sync.event.SyncLogEvent;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.search.service.ProductSearchMapService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final CardProductRepository cardProductRepository;
    private final ProductSearchMapService productSearchMapService;
    private final ApplicationEventPublisher eventPublisher;

    @Lazy
    @Autowired
    private StockService self;

    private final AtomicBoolean stockSyncRunning = new AtomicBoolean(false);
    private volatile CompletableFuture<Void> stockSyncTask;

    @Override
    @Transactional
    public void syncStock() {
        int updated = cardProductRepository.syncStock();
        List<CardProduct> syncedProducts = cardProductRepository.findAutoUpdatedVisibleWithUnionPrice();
        productSearchMapService.syncProductSearchMapBulk(syncedProducts);
        log.info("Stock sync complete: {} rows updated", updated);
    }

    @Override
    public boolean startSyncStock(String logSource) {
        if (!stockSyncRunning.compareAndSet(false, true)) {
            log.warn("Stock sync is already running. Skip this request. source={}", logSource);
            return false;
        }
        stockSyncTask = CompletableFuture.runAsync(() -> {
            try {
                runStockSyncJob(logSource);
            } finally {
                stockSyncRunning.set(false);
                stockSyncTask = null;
            }
        });
        return true;
    }

    @Override
    public void runStockSyncJob(String logSource) {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;
        String result = "failure";
        String message = "";
        try {
            // self 인 이유: 재귀적으로 재고 충전을 실행하기 위해(트랜잭셔널 정상 적용을 위해 필요함)
            self.syncStock();
            result = "success";
        } catch (Exception e) {
            message = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
            log.error("Stock sync job failed. source={}", logSource, e);
        } finally {
            endTime = LocalDateTime.now();
            eventPublisher.publishEvent(new SyncLogEvent("stock", logSource, startTime, endTime, result, message));
        }
    }
}
