package com.shop.order.adjustment.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shop.offline.product.service.OfflineOnlineShippingQuantityAdjuster;
import com.shop.order.adjustment.StockRestoreEntry;
import com.shop.order.adjustment.stock.OrderStockReleaseHandler;
import com.shop.order.adjustment.stock.OrderStockReleaseHandlerRegistry;
import com.shop.order.entity.OrderProduct;
import com.shop.order.repository.OrderProductRepository;
import com.shop.search.dto.enums.ProductTableEnum;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderStockRestorationService {

    private final OrderStockReleaseHandlerRegistry handlerRegistry;
    private final OrderProductRepository orderProductRepository;
    private final OfflineOnlineShippingQuantityAdjuster offlineOnlineShippingQuantityAdjuster;

    public void restoreForLine(OrderProduct line, long qty) {
        if (qty <= 0) {
            return;
        }
        ProductTableEnum table = line.getProductTable();
        if (table == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "STOCK_RESTORE_FAILED");
        }

        OrderStockReleaseHandler handler = handlerRegistry.find(table).orElse(null);
        if (handler == null) {
            return;
        }

        int updated = handler.releaseStock(line.getProductId(), qty);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "STOCK_RESTORE_FAILED");
        }
        handler.publishStockSync(line.getProductId());
        offlineOnlineShippingQuantityAdjuster.decreaseForOrderLine(table, line.getProductId(), (int) qty);
    }

    public void restoreAllForOrder(Long orderInfoId) {
        for (OrderProduct line : orderProductRepository.findByOrderInfoId(orderInfoId)) {
            long qty = line.getQuantity() != null ? line.getQuantity() : 0L;
            if (qty > 0) {
                restoreForLine(line, qty);
            }
        }
    }

    public void restoreEntries(Iterable<StockRestoreEntry> entries) {
        for (StockRestoreEntry entry : entries) {
            restoreForLine(entry.getLine(), entry.getQuantity());
        }
    }
}
