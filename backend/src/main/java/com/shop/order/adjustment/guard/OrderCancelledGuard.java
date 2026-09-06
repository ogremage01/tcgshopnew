package com.shop.order.adjustment.guard;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.shop.order.entity.OrderInfo;
import com.shop.order.enums.OrderStatus;

@Component
public class OrderCancelledGuard {

    public void assertNotCancelled(OrderInfo orderInfo) {
        if (OrderStatus.ORDER_CANCELLED.getValue().equals(orderInfo.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ORDER_ALREADY_CANCELLED");
        }
    }
}
