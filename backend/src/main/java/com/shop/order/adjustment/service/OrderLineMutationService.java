package com.shop.order.adjustment.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.order.dto.AdminOrderProductModifyItem;
import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.StockRestoreEntry;
import com.shop.order.entity.OrderProduct;
import com.shop.order.repository.OrderProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderLineMutationService {

    private final OrderProductRepository orderProductRepository;

    public List<StockRestoreEntry> mutateLines(Long orderId, List<AdminOrderProductModifyItem> items) {
        List<StockRestoreEntry> stockEntries = new ArrayList<>();

        for (AdminOrderProductModifyItem item : items) {
            OrderProduct line = orderProductRepository.findById(item.getOrderProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_PRODUCT_NOT_FOUND"));

            if (!line.getOrderInfoId().equals(orderId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_NOT_BELONG_TO_ORDER");
            }

            if (item.isDeleted()) {
                long qty = line.getQuantity() != null ? line.getQuantity() : 0L;
                if (item.isRestoreStock() && qty > 0) {
                    stockEntries.add(new StockRestoreEntry(line, qty));
                }
                orderProductRepository.delete(line);
            } else if (item.getNewQuantity() != null) {
                long currentQty = line.getQuantity() != null ? line.getQuantity() : 0L;
                long newQty = item.getNewQuantity();
                if (newQty >= currentQty) {
                    continue;
                }
                if (newQty <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY");
                }
                long diff = currentQty - newQty;
                if (item.isRestoreStock()) {
                    stockEntries.add(new StockRestoreEntry(line, diff));
                }
                long unitPrice = line.getSnapshotUnitPrice() != null ? line.getSnapshotUnitPrice() : 0L;
                line.setQuantity(newQty);
                line.setTotalPrice(BigDecimal.valueOf(unitPrice * newQty));
                orderProductRepository.save(line);
            }
        }

        return stockEntries;
    }
}
