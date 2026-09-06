package com.shop.order.adjustment.stock;

import com.shop.search.dto.enums.ProductTableEnum;

public interface OrderStockReleaseHandler {

    ProductTableEnum table();

    int releaseStock(Long productId, long quantity);

    void publishStockSync(Long productId);
}
