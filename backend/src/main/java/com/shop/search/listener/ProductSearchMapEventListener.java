package com.shop.search.listener;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.shop.search.event.CardProductBulkChangeEvent;
import com.shop.search.event.CardProductChangeByStorageEvent;
import com.shop.search.event.CardProductChangeEvent;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.search.event.ManualProductChangeEvent;
import com.shop.search.event.SealedProductChangeEvent;
import com.shop.search.service.ProductSearchMapService;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchMapEventListener {

    private static final String LOG_PREFIX = "[PSM-STOCK]";

    private final ProductSearchMapService productSearchMapService;
    private final CardProductRepository cardProductRepository;
    private final ManualProductRepository manualProductRepository;
    private final SealedProductRepository sealedProductRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCardProductChangeEvent(CardProductChangeEvent event) {
        CardProduct fromEvent = event.getCardProduct();
        if (fromEvent == null || fromEvent.getId() == null) {
            log.warn("{} listener CardProductChangeEvent skipped: empty event", LOG_PREFIX);
            return;
        }
        log.info("{} listener CardProductChangeEvent cardProductId={}", LOG_PREFIX, fromEvent.getId());
        cardProductRepository.findByIdWithUnionPrice(fromEvent.getId())
                .or(() -> cardProductRepository.findById(fromEvent.getId()))
                .ifPresentOrElse(
                        productSearchMapService::syncProductSearchMap,
                        () -> log.warn("{} listener CardProduct not found id={}", LOG_PREFIX, fromEvent.getId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCardProductBulkChangeEvent(CardProductBulkChangeEvent event) {
        productSearchMapService.syncProductSearchMapBulk(event.getCardProducts());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCardProductChangeByStorageEvent(CardProductChangeByStorageEvent event) {
        productSearchMapService.isvisibleChangeProductSearchMapByStorage(event.getStorageId());
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleManualProductChangeEvent(ManualProductChangeEvent event) {
        ManualProduct fromEvent = event.getManualProduct();
        if (fromEvent == null || fromEvent.getId() == null) {
            log.warn("{} listener ManualProductChangeEvent skipped: empty event", LOG_PREFIX);
            return;
        }
        log.info("{} listener ManualProductChangeEvent manualProductId={}", LOG_PREFIX, fromEvent.getId());
        manualProductRepository.findById(fromEvent.getId())
                .ifPresentOrElse(
                        productSearchMapService::syncProductSearchMap,
                        () -> log.warn("{} listener ManualProduct not found id={}", LOG_PREFIX, fromEvent.getId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSealedProductChangeEvent(SealedProductChangeEvent event) {
        SealedProduct fromEvent = event.getSealedProduct();
        if (fromEvent == null || fromEvent.getId() == null) {
            log.warn("{} listener SealedProductChangeEvent skipped: empty event", LOG_PREFIX);
            return;
        }
        log.info("{} listener SealedProductChangeEvent sealedProductId={}", LOG_PREFIX, fromEvent.getId());
        sealedProductRepository.findById(fromEvent.getId())
                .ifPresentOrElse(
                        productSearchMapService::syncProductSearchMap,
                        () -> log.warn("{} listener SealedProduct not found id={}", LOG_PREFIX, fromEvent.getId()));
    }
}
