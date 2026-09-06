package com.shop.order.adjustment.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.point.OrderEarnedPointCalculator;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderAmountRecalculationService {

    private final OrderProductRepository orderProductRepository;
    private final OrderInfoRepository orderInfoRepository;

    public void recalculate(OrderAdjustmentContext ctx) {
        OrderInfo orderInfo = ctx.getOrderInfo();
        List<OrderProduct> remaining;

        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            remaining = orderProductRepository.findByOrderInfoId(ctx.getOrderId());
            applyZeroAmounts(orderInfo, remaining);
            ctx.setNewUsedPointAmount(BigDecimal.ZERO);
            ctx.setNewActualPaymentAmount(BigDecimal.ZERO);
            ctx.setNewEarnedPointAmount(0L);
            ctx.setReleasedUsedPoints(toLong(ctx.getOriginalUsedPointAmount()));
            ctx.setPgRefundAmount(ctx.getOriginalActualPaymentAmount());
            ctx.setEarnedPointDelta(ctx.getOriginalEarnedPointAmount());
        } else {
            remaining = orderProductRepository.findByOrderInfoId(ctx.getOrderId());
            if (remaining.isEmpty()) {
                applyZeroAmounts(orderInfo, remaining);
                ctx.setNewUsedPointAmount(BigDecimal.ZERO);
                ctx.setNewActualPaymentAmount(BigDecimal.ZERO);
                ctx.setNewEarnedPointAmount(0L);
                ctx.setReleasedUsedPoints(toLong(ctx.getOriginalUsedPointAmount()));
                ctx.setPgRefundAmount(ctx.getOriginalActualPaymentAmount());
                ctx.setEarnedPointDelta(ctx.getOriginalEarnedPointAmount());
            } else {
                RecalcResult result = computeFromLines(orderInfo, remaining, ctx.getOriginalUsedPointAmount(),
                        ctx.getOriginalActualPaymentAmount(), ctx.getOriginalEarnedPointAmount());
                applyResult(orderInfo, remaining, result);
                ctx.setReleasedUsedPoints(result.releasedUsedPoints());
                ctx.setPgRefundAmount(result.pgRefundAmount());
                ctx.setEarnedPointDelta(result.earnedPointDelta());
                ctx.setNewUsedPointAmount(result.newUsedPointAmount());
                ctx.setNewActualPaymentAmount(result.newActualPaymentAmount());
                ctx.setNewEarnedPointAmount(result.newEarnedPointAmount());
            }
        }

        ctx.setRemainingLines(remaining);
        orderInfoRepository.save(orderInfo);
    }

    private void applyZeroAmounts(OrderInfo orderInfo, List<OrderProduct> remaining) {
        long totalQuantity = remaining.stream()
                .mapToLong(p -> p.getQuantity() != null ? p.getQuantity() : 0L)
                .sum();

        orderInfo.setTotalProductAmount(BigDecimal.ZERO);
        orderInfo.setDeliveryFee(BigDecimal.ZERO);
        orderInfo.setUsedPointAmount(BigDecimal.ZERO);
        orderInfo.setActualPaymentAmount(BigDecimal.ZERO);
        orderInfo.setTotalQuantity(totalQuantity);
        orderInfo.setOrderLineCount((long) remaining.size());
        orderInfo.setEarnedPointAmount(0L);
    }

    private RecalcResult computeFromLines(
            OrderInfo orderInfo,
            List<OrderProduct> remaining,
            BigDecimal originalUsedPoint,
            BigDecimal originalActualPayment,
            long originalEarnedPoint) {

        BigDecimal newTotalProductAmount = remaining.stream()
                .map(p -> p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long newTotalQuantity = remaining.stream()
                .mapToLong(p -> p.getQuantity() != null ? p.getQuantity() : 0L)
                .sum();
        long newOrderLineCount = remaining.size();

        BigDecimal deliveryFee = orderInfo.getDeliveryFee() != null ? orderInfo.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal usedPointAmount = orderInfo.getUsedPointAmount() != null
                ? orderInfo.getUsedPointAmount() : BigDecimal.ZERO;

        BigDecimal maxUsable = newTotalProductAmount.add(deliveryFee);
        if (usedPointAmount.compareTo(maxUsable) > 0) {
            usedPointAmount = maxUsable;
        }

        BigDecimal newActualPaymentAmount = newTotalProductAmount.add(deliveryFee).subtract(usedPointAmount);
        long newEarnedPointAmount = OrderEarnedPointCalculator.totalEarnedPoints(remaining);

        long releasedUsedPoints = toLong(originalUsedPoint) - toLong(usedPointAmount);
        BigDecimal pgRefundAmount = originalActualPayment.subtract(newActualPaymentAmount);
        long earnedPointDelta = originalEarnedPoint - newEarnedPointAmount;

        return new RecalcResult(
                newTotalProductAmount,
                usedPointAmount,
                newActualPaymentAmount,
                newTotalQuantity,
                newOrderLineCount,
                newEarnedPointAmount,
                releasedUsedPoints,
                pgRefundAmount,
                earnedPointDelta);
    }

    private void applyResult(OrderInfo orderInfo, List<OrderProduct> remaining, RecalcResult result) {
        orderInfo.setTotalProductAmount(result.newTotalProductAmount());
        orderInfo.setUsedPointAmount(result.newUsedPointAmount());
        orderInfo.setActualPaymentAmount(result.newActualPaymentAmount());
        orderInfo.setTotalQuantity(result.newTotalQuantity());
        orderInfo.setOrderLineCount(result.newOrderLineCount());
        orderInfo.setEarnedPointAmount(result.newEarnedPointAmount());
    }

    private static long toLong(BigDecimal value) {
        return value != null ? value.longValue() : 0L;
    }

    private record RecalcResult(
            BigDecimal newTotalProductAmount,
            BigDecimal newUsedPointAmount,
            BigDecimal newActualPaymentAmount,
            long newTotalQuantity,
            long newOrderLineCount,
            long newEarnedPointAmount,
            long releasedUsedPoints,
            BigDecimal pgRefundAmount,
            long earnedPointDelta) {
    }
}
