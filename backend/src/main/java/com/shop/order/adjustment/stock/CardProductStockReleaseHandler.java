package com.shop.order.adjustment.stock;

import org.springframework.stereotype.Component;

import com.shop.product.repository.card.CardProductRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CardProductStockReleaseHandler implements OrderStockReleaseHandler {

    private final CardProductRepository cardProductRepository;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.CARD_PRODUCT;
    }

    @Override
    public int releaseStock(Long productId, long quantity) {
        return cardProductRepository.restoreStock(productId, quantity);
    }

    @Override
    public void publishStockSync(Long productId) {
        productSearchMapStockSyncPublisher.publishCardProductStockChanged(productId);
    }
}
