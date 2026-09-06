package com.shop.order.adjustment.stock;

import org.springframework.stereotype.Component;

import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SealedProductStockReleaseHandler implements OrderStockReleaseHandler {

    private final SealedProductRepository sealedProductRepository;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.SEALED_PRODUCT;
    }

    @Override
    public int releaseStock(Long productId, long quantity) {
        return sealedProductRepository.restoreStock(productId, quantity);
    }

    @Override
    public void publishStockSync(Long productId) {
        productSearchMapStockSyncPublisher.publishSealedProductStockChanged(productId);
    }
}
