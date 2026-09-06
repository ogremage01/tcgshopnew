package com.shop.order.adjustment.step;

import com.shop.order.adjustment.OrderAdjustmentContext;

public interface OrderAdjustmentStep {

    void run(OrderAdjustmentContext ctx);
}
