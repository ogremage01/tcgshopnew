package com.shop.order.adjustment.step;

import java.util.List;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.StockRestoreEntry;
import com.shop.order.adjustment.service.OrderLineMutationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LineMutationStep implements OrderAdjustmentStep {

    private final OrderLineMutationService orderLineMutationService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        if (ctx.getType() != OrderAdjustmentType.PARTIAL_MODIFY) {
            return;
        }
        List<StockRestoreEntry> entries = orderLineMutationService.mutateLines(
                ctx.getOrderId(),
                ctx.getModifyRequest().getItems());
        ctx.setStockRestoreEntries(entries);
    }
}
