package com.shop.order.adjustment.step;

import org.springframework.stereotype.Component;

import com.shop.cart.dto.CartRestoreResult;
import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.service.OrderCartRestorationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartRestoreStep implements OrderAdjustmentStep {

    private final OrderCartRestorationService orderCartRestorationService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        if (ctx.getType() != OrderAdjustmentType.FULL_CANCEL || !ctx.isRestoreCartOnCancel()) {
            return;
        }

        ctx.setCartRestoreRequested(true);
        CartRestoreResult result = orderCartRestorationService.restoreAllForOrder(
                ctx.getOrderInfo(), ctx.getOrderId());

        ctx.setCartRestoreApplied(result.isApplied());
        ctx.setCartLinesRequested(result.getLinesRequested());
        ctx.setCartLinesFullyRestored(result.getLinesFullyRestored());
        ctx.setCartLinesPartiallyRestored(result.getLinesPartiallyRestored());
        ctx.setCartLinesSkipped(result.getLinesSkipped());
    }
}
