package com.shop.order.adjustment.step;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.service.OrderPaymentRefundService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentRefundStep implements OrderAdjustmentStep {

    private final OrderPaymentRefundService orderPaymentRefundService;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        ctx.setPgRefundSuccess(true);
        ctx.setPgRefundMessage(null);

        BigDecimal refundAmount = resolveRefundAmount(ctx);
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (!OrderPaymentRefundService.requiresTossRefund(ctx.getOrderInfo(), refundAmount)) {
            return;
        }

        String cancelReason = ctx.getCancelReason();

        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            orderPaymentRefundService.refundOrThrow(ctx.getOrderInfo(), refundAmount, cancelReason);
            return;
        }

        orderPaymentRefundService.refundPartialOrThrow(ctx.getOrderInfo(), refundAmount, cancelReason);
    }

    private static BigDecimal resolveRefundAmount(OrderAdjustmentContext ctx) {
        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            return ctx.getOriginalActualPaymentAmount();
        }
        return ctx.getPgRefundAmount();
    }
}
