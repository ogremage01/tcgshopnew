package com.shop.order.adjustment.step;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.order.dto.AdminOrderProductModifyItem;
import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.guard.OrderCancelledGuard;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ValidateStep implements OrderAdjustmentStep {

    private final OrderInfoRepository orderInfoRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderCancelledGuard orderCancelledGuard;

    @Override
    public void run(OrderAdjustmentContext ctx) {
        OrderInfo orderInfo = orderInfoRepository.findById(ctx.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND"));

        orderCancelledGuard.assertNotCancelled(orderInfo);
        ctx.setOrderInfo(orderInfo);

        ctx.setOriginalUsedPointAmount(
                orderInfo.getUsedPointAmount() != null ? orderInfo.getUsedPointAmount() : java.math.BigDecimal.ZERO);
        ctx.setOriginalActualPaymentAmount(
                orderInfo.getActualPaymentAmount() != null ? orderInfo.getActualPaymentAmount() : java.math.BigDecimal.ZERO);
        ctx.setOriginalEarnedPointAmount(
                orderInfo.getEarnedPointAmount() != null ? orderInfo.getEarnedPointAmount() : 0L);

        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            String reason = ctx.getCancelReason();
            if (reason == null || reason.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANCEL_REASON_REQUIRED");
            }
            return;
        }

        if (ctx.getType() != OrderAdjustmentType.PARTIAL_MODIFY) {
            return;
        }

        String partialReason = ctx.getCancelReason();
        if (partialReason == null || partialReason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANCEL_REASON_REQUIRED");
        }

        if (ctx.getModifyRequest() == null || ctx.getModifyRequest().getItems() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_MODIFY_REQUEST");
        }

        for (AdminOrderProductModifyItem item : ctx.getModifyRequest().getItems()) {
            OrderProduct line = orderProductRepository.findById(item.getOrderProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_PRODUCT_NOT_FOUND"));

            if (!line.getOrderInfoId().equals(ctx.getOrderId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_NOT_BELONG_TO_ORDER");
            }

            if (!item.isDeleted() && item.getNewQuantity() != null) {
                long currentQty = line.getQuantity() != null ? line.getQuantity() : 0L;
                long newQty = item.getNewQuantity();
                if (newQty < currentQty && newQty <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY");
                }
            }
        }
    }
}
