package com.shop.order.adjustment.step;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.service.OrderAmountRecalculationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AmountRecalcStep implements OrderAdjustmentStep {

    private final OrderAmountRecalculationService orderAmountRecalculationService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        orderAmountRecalculationService.recalculate(ctx);
    }
}
