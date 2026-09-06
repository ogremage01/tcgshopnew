package com.shop.order.point;

import java.util.List;

import com.shop.order.entity.OrderProduct;

public final class OrderEarnedPointCalculator {

    private OrderEarnedPointCalculator() {
    }

    public static long lineEarnedPoints(OrderProduct op) {
        if (op == null) {
            return 0L;
        }
        long unit = op.getRewardPoints() != null ? op.getRewardPoints() : 0L;
        long qty = op.getQuantity() != null ? op.getQuantity() : 1L;
        return unit * qty;
    }

    public static long totalEarnedPoints(List<OrderProduct> lines) {
        if (lines == null || lines.isEmpty()) {
            return 0L;
        }
        return lines.stream().mapToLong(OrderEarnedPointCalculator::lineEarnedPoints).sum();
    }

    public static String earnChangeReason(long orderId) {
        return "ORDER_EARN_POINTS:" + orderId;
    }
}
