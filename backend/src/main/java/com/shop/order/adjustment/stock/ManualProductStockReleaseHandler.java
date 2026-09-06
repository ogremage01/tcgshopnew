package com.shop.order.adjustment.stock;

import org.springframework.stereotype.Component;

import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ManualProductStockReleaseHandler implements OrderStockReleaseHandler {

    private final ManualProductRepository manualProductRepository;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.MANUAL_PRODUCT;
    }

    @Override
    public int releaseStock(Long productId, long quantity) {
        return manualProductRepository.restoreStock(productId, quantity);
    }

    @Override
    public void publishStockSync(Long productId) {
        productSearchMapStockSyncPublisher.publishManualProductStockChanged(productId);
    }
}
