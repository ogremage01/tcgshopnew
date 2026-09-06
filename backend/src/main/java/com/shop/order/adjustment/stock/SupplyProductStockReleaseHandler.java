package com.shop.order.adjustment.stock;

import org.springframework.stereotype.Component;

import com.shop.product.repository.supply.SupplyRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupplyProductStockReleaseHandler implements OrderStockReleaseHandler {

    private final SupplyRepository supplyRepository;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.SUPPLY;
    }

    @Override
    public int releaseStock(Long productId, long quantity) {
        return supplyRepository.restoreStock(productId, quantity);
    }

    @Override
    public void publishStockSync(Long productId) {
        productSearchMapStockSyncPublisher.publishSupplyStockChanged(productId);
    }
}
