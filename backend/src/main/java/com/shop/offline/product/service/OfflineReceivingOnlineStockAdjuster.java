package com.shop.offline.product.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 링크된 온라인 상품 재고를 OfflineProduct 재고(입고 − 출고)로 동기화한다.
 * 입고 − 출고가 0 이하이면 온라인 재고는 변경하지 않는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineReceivingOnlineStockAdjuster {

    private final ManualProductRepository manualProductRepository;
    private final SupplyRepository supplyRepository;
    private final SealedProductRepository sealedProductRepository;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    /**
     * 양수 입고 후 동기화. OfflineProduct.receivingQuantity가 이미 반영된 뒤 호출한다.
     */
    public void applyReceivingIncrease(OfflineProduct offlineProduct, int receivingQuantity) {
        if (receivingQuantity < 1) {
            return;
        }
        syncOnlineStockFromOffline(offlineProduct);
    }

    /**
     * 입고 델타 반영 후 동기화. qty=0이면 skip.
     * OfflineProduct 수량이 이미 갱신된 상태여야 한다.
     */
    public void applyReceivingDelta(OfflineProduct offlineProduct, int qty) {
        if (offlineProduct == null || qty == 0) {
            return;
        }
        syncOnlineStockFromOffline(offlineProduct);
    }

    /**
     * 링크(linkTableName + linkId)가 있으면 온라인 재고를
     * (receivingQuantity − shippingQuantity)로 SET 한다.
     * 계산값이 0 이하면 온라인 재고는 건드리지 않는다.
     */
    public void syncOnlineStockFromOffline(OfflineProduct offlineProduct) {
        if (offlineProduct == null) {
            return;
        }
        String linkTable = offlineProduct.getLinkTableName();
        Long linkId = offlineProduct.getLinkId();
        if (!StringUtils.hasText(linkTable) || linkId == null) {
            return;
        }

        int targetStock = availableStock(offlineProduct);
        if (targetStock <= 0) {
            return;
        }
        switch (linkTable) {
            case "Manual" -> syncManualStock(linkId, targetStock);
            case "Supply" -> syncSupplyStock(linkId, targetStock);
            case "Sealed" -> syncSealedStock(linkId, targetStock);
            default -> log.warn(
                    "온라인 재고 동기화 skip — 알 수 없는 linkTableName: offlineProductId={}, linkTable={}, linkId={}",
                    offlineProduct.getId(), linkTable, linkId);
        }
    }

    /** 입고 − 출고. null은 0으로 취급하며, 음수도 그대로 반환한다. */
    static int availableStock(OfflineProduct offlineProduct) {
        int receiving = offlineProduct.getReceivingQuantity() != null
                ? offlineProduct.getReceivingQuantity()
                : 0;
        int shipping = offlineProduct.getShippingQuantity() != null
                ? offlineProduct.getShippingQuantity()
                : 0;
        return receiving - shipping;
    }

    private void syncManualStock(Long linkId, int targetStock) {
        ManualProduct product = manualProductRepository.findById(linkId).orElse(null);
        if (product == null) {
            log.warn("온라인 재고 동기화 실패 — ManualProduct 없음: linkId={}", linkId);
            return;
        }
        long current = product.getStock() != null ? product.getStock() : 0L;
        if (current == targetStock) {
            return;
        }
        product.setStock((long) targetStock);
        manualProductRepository.save(product);
        productSearchMapStockSyncPublisher.publishManualProductStockChanged(linkId);
    }

    private void syncSupplyStock(Long linkId, int targetStock) {
        Supply product = supplyRepository.findById(linkId).orElse(null);
        if (product == null) {
            log.warn("온라인 재고 동기화 실패 — Supply 없음: linkId={}", linkId);
            return;
        }
        long current = product.getStock() != null ? product.getStock() : 0L;
        if (current == targetStock) {
            return;
        }
        product.setStock((long) targetStock);
        supplyRepository.save(product);
        productSearchMapStockSyncPublisher.publishSupplyStockChanged(linkId);
    }

    /**
     * Sealed: totalStock = target,
     * currentVisibleStock = min(target, maxVisibleStock 또는 target),
     * maxVisibleStock 유지.
     */
    private void syncSealedStock(Long linkId, int targetStock) {
        SealedProduct product = sealedProductRepository.findById(linkId).orElse(null);
        if (product == null) {
            log.warn("온라인 재고 동기화 실패 — SealedProduct 없음: linkId={}", linkId);
            return;
        }
        int total = product.getTotalStock() != null ? product.getTotalStock() : 0;
        Integer configuredMaxVisible = product.getMaxVisibleStock();
        int maxVisible = configuredMaxVisible != null ? configuredMaxVisible : targetStock;
        int newVisible = Math.min(targetStock, Math.max(0, maxVisible));
        int currentVisible = product.getCurrentVisibleStock() != null ? product.getCurrentVisibleStock() : 0;
        if (total == targetStock && currentVisible == newVisible) {
            return;
        }
        product.setTotalStock(targetStock);
        product.setCurrentVisibleStock(newVisible);
        sealedProductRepository.save(product);
        productSearchMapStockSyncPublisher.publishSealedProductStockChanged(linkId);
    }
}
