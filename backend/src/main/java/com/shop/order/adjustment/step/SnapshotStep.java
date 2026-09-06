package com.shop.order.adjustment.step;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.service.OrderSnapshotService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SnapshotStep implements OrderAdjustmentStep {

    private final OrderSnapshotService orderSnapshotService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        if (ctx.getType() != OrderAdjustmentType.PARTIAL_MODIFY) {
            return;
        }
        Long snapshotId = orderSnapshotService.saveSnapshot(ctx.getOrderInfo());
        ctx.setSnapshotId(snapshotId);
    }
}
