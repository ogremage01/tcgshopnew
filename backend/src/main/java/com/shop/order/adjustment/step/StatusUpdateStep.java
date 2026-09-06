package com.shop.order.adjustment.step;

import org.springframework.stereotype.Component;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.enums.OrderStatus;
import com.shop.order.payment.OrderPaymentStatuses;
import com.shop.order.repository.OrderInfoRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StatusUpdateStep implements OrderAdjustmentStep {

    private final OrderInfoRepository orderInfoRepository;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            markCancelled(ctx);
            return;
        }

        if (ctx.getRemainingLines() == null || ctx.getRemainingLines().isEmpty()) {
            markCancelled(ctx);
        }
    }

    private void markCancelled(OrderAdjustmentContext ctx) {
        ctx.getOrderInfo().setOrderStatus(OrderStatus.ORDER_CANCELLED.getValue());
        if (OrderPaymentStatuses.PAYMENT_COMPLETED.equals(ctx.getOrderInfo().getPaymentStatus())) {
            ctx.getOrderInfo().setPaymentStatus(OrderPaymentStatuses.PAYMENT_CANCELLED);
        }
        ctx.setOrderCancelled(true);
        orderInfoRepository.save(ctx.getOrderInfo());
    }
}
