package com.shop.order.adjustment.step;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.service.OrderStockRestorationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StockRestoreStep implements OrderAdjustmentStep {

    private final OrderStockRestorationService orderStockRestorationService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            if (ctx.isRestoreStockOnCancel()) {
                orderStockRestorationService.restoreAllForOrder(ctx.getOrderId());
            }
            return;
        }

        orderStockRestorationService.restoreEntries(ctx.getStockRestoreEntries());
    }
}
