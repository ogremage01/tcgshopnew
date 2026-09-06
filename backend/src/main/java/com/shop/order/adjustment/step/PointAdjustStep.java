package com.shop.order.adjustment.step;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.service.OrderPointAdjustmentService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PointAdjustStep implements OrderAdjustmentStep {

    private final OrderPointAdjustmentService orderPointAdjustmentService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        orderPointAdjustmentService.adjust(ctx);
    }
}
