package com.shop.offline.product.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.search.dto.enums.ProductTableEnum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 온라인 주문 확정/재고 복구 시 링크된 OfflineProduct.shippingQuantity를 가감한다.
 * 매칭 키는 온라인 상품의 offlineProductId → OfflineProduct.productId.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineOnlineShippingQuantityAdjuster {

    private final OfflineProductRepository offlineProductRepository;
    private final SealedProductRepository sealedProductRepository;
    private final ManualProductRepository manualProductRepository;
    private final SupplyRepository supplyRepository;
    private final OfflinePackagingConversionService packagingConversionService;
    private final OfflineReceivingOnlineStockAdjuster receivingOnlineStockAdjuster;

    public void increaseForOrderLine(ProductTableEnum table, Long productId, int quantity) {
        applyDelta(table, productId, quantity);
    }

    public void decreaseForOrderLine(ProductTableEnum table, Long productId, int quantity) {
        applyDelta(table, productId, -quantity);
    }

    private void applyDelta(ProductTableEnum table, Long productId, int delta) {
        if (table == null || productId == null || delta == 0) {
            return;
        }

        String offlineProductId = resolveOfflineProductId(table, productId);
        if (!StringUtils.hasText(offlineProductId)) {
            return;
        }

        OfflineProduct offlineProduct = offlineProductRepository.findByProductId(offlineProductId).orElse(null);
        if (offlineProduct == null) {
            log.warn(
                    "온라인 출고 반영 skip — OfflineProduct 없음: table={}, productId={}, offlineProductId={}, delta={}",
                    table, productId, offlineProductId, delta);
            return;
        }

        int current = offlineProduct.getShippingQuantity() != null ? offlineProduct.getShippingQuantity() : 0;
        offlineProduct.setShippingQuantity(current + delta);
        offlineProductRepository.save(offlineProduct);
        if (delta > 0) {
            packagingConversionService.ensurePackagingStockAfterShipping(offlineProduct);
        }
        receivingOnlineStockAdjuster.syncOnlineStockFromOffline(offlineProduct);
    }

    private String resolveOfflineProductId(ProductTableEnum table, Long productId) {
        return switch (table) {
            case SEALED_PRODUCT -> sealedProductRepository.findById(productId)
                    .map(SealedProduct::getOfflineProductId)
                    .filter(StringUtils::hasText)
                    .orElse(null);
            case MANUAL_PRODUCT -> manualProductRepository.findById(productId)
                    .map(ManualProduct::getOfflineProductId)
                    .filter(StringUtils::hasText)
                    .orElse(null);
            case SUPPLY -> supplyRepository.findById(productId)
                    .map(Supply::getOfflineProductId)
                    .filter(StringUtils::hasText)
                    .orElse(null);
            default -> null;
        };
    }
}
