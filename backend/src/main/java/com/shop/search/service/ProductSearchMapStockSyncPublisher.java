package com.shop.search.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.product.repository.supply.SupplyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 재고 차감·복구 등 ProductSearchMap 동기화가 누락되는 경로에서
 * 커밋 이후 {@link ProductSearchMapService} 갱신을 트리거한다.
 *
 * <p>주문 확정 트랜잭션 안에서는 {@code current_visible_stock}이 아직 커밋되지 않았을 수 있으므로
 * {@link TransactionSynchronization#afterCommit()}에서 동기화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchMapStockSyncPublisher {

    private static final String LOG_PREFIX = "[PSM-STOCK]";

    private final CardProductRepository cardProductRepository;
    private final ManualProductRepository manualProductRepository;
    private final SealedProductRepository sealedProductRepository;
    private final ProductSearchMapService productSearchMapService;
    private final SupplyRepository supplyRepository;
    public void publishCardProductStockChanged(Long cardProductId) {
        if (cardProductId == null) {
            log.warn("{} publishCardProductStockChanged skipped: cardProductId is null", LOG_PREFIX);
            return;
        }
        log.info("{} schedule card stock sync afterCommit cardProductId={}", LOG_PREFIX, cardProductId);
        runAfterCommit(() -> syncCardProductSearchMap(cardProductId));
    }

    public void publishManualProductStockChanged(Long manualProductId) {
        if (manualProductId == null) {
            log.warn("{} publishManualProductStockChanged skipped: manualProductId is null", LOG_PREFIX);
            return;
        }
        log.info("{} schedule manual stock sync afterCommit manualProductId={}", LOG_PREFIX, manualProductId);
        runAfterCommit(() -> syncManualProductSearchMap(manualProductId));
    }

    public void publishSealedProductStockChanged(Long sealedProductId) {
        if (sealedProductId == null) {
            log.warn("{} publishSealedProductStockChanged skipped: sealedProductId is null", LOG_PREFIX);
            return;
        }
        log.info("{} schedule sealed stock sync afterCommit sealedProductId={}", LOG_PREFIX, sealedProductId);
        runAfterCommit(() -> syncSealedProductSearchMap(sealedProductId));
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("{} afterCommit fired (tx={})", LOG_PREFIX,
                            TransactionSynchronizationManager.getCurrentTransactionName());
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.error("{} afterCommit sync failed", LOG_PREFIX, e);
                    }
                }
            });
            log.info("{} afterCommit callback registered", LOG_PREFIX);
            return;
        }
        log.warn("{} no active transaction; running stock sync immediately", LOG_PREFIX);
        try {
            action.run();
        } catch (Exception e) {
            log.error("{} immediate stock sync failed", LOG_PREFIX, e);
        }
    }

    private void syncCardProductSearchMap(Long cardProductId) {
        log.info("{} syncCardProductSearchMap start cardProductId={}", LOG_PREFIX, cardProductId);
        cardProductRepository.findByIdWithUnionPrice(cardProductId)
                .or(() -> cardProductRepository.findById(cardProductId))
                .ifPresentOrElse(cp -> {
                    log.info(
                            "{} CardProduct loaded id={} publicId={} currentVisibleStock={} isVisible={}",
                            LOG_PREFIX,
                            cp.getId(),
                            cp.getPublicId(),
                            cp.getCurrentVisibleStock(),
                            cp.getIsVisible());
                    productSearchMapService.syncProductSearchMap(cp);
                    log.info("{} syncCardProductSearchMap done cardProductId={}", LOG_PREFIX, cardProductId);
                }, () -> log.warn("{} CardProduct not found, sync skipped id={}", LOG_PREFIX, cardProductId));
    }

    private void syncManualProductSearchMap(Long manualProductId) {
        log.info("{} syncManualProductSearchMap start manualProductId={}", LOG_PREFIX, manualProductId);
        manualProductRepository.findById(manualProductId)
                .ifPresentOrElse(mp -> {
                    log.info(
                            "{} ManualProduct loaded id={} publicId={} stock={} isVisible={}",
                            LOG_PREFIX,
                            mp.getId(),
                            mp.getPublicId(),
                            mp.getStock(),
                            mp.getIsVisible());
                    productSearchMapService.syncProductSearchMap(mp);
                    log.info("{} syncManualProductSearchMap done manualProductId={}", LOG_PREFIX, manualProductId);
                }, () -> log.warn("{} ManualProduct not found, sync skipped id={}", LOG_PREFIX, manualProductId));
    }

    private void syncSealedProductSearchMap(Long sealedProductId) {
        log.info("{} syncSealedProductSearchMap start sealedProductId={}", LOG_PREFIX, sealedProductId);
        sealedProductRepository.findById(sealedProductId)
                .ifPresentOrElse(sp -> {
                    log.info(
                            "{} SealedProduct loaded id={} publicId={} currentVisibleStock={} isActive={}",
                            LOG_PREFIX,
                            sp.getId(),
                            sp.getPublicId(),
                            sp.getCurrentVisibleStock(),
                            sp.getIsActive());
                    productSearchMapService.syncProductSearchMap(sp);
                    log.info("{} syncSealedProductSearchMap done sealedProductId={}", LOG_PREFIX, sealedProductId);
                }, () -> log.warn("{} SealedProduct not found, sync skipped id={}", LOG_PREFIX, sealedProductId));
    }

    public void publishSupplyStockChanged(Long supplyId) {
        log.info("{} publishSupplyStockChanged start supplyId={}", LOG_PREFIX, supplyId);
        runAfterCommit(() -> syncSupplySearchMap(supplyId));
    }

    private void syncSupplySearchMap(Long supplyId) {
        log.info("{} syncSupplySearchMap start supplyId={}", LOG_PREFIX, supplyId);
        supplyRepository.findById(supplyId)
                .ifPresentOrElse(s -> {
                    log.info("{} Supply loaded id={} publicId={} stock={} isVisible={}", LOG_PREFIX, s.getId(), s.getPublicId(), s.getStock(), s.getIsVisible());
                    productSearchMapService.syncProductSearchMap(s);
                    log.info("{} syncSupplySearchMap done supplyId={}", LOG_PREFIX, supplyId);
                }, () -> log.warn("{} Supply not found, sync skipped id={}", LOG_PREFIX, supplyId));
    }
}
