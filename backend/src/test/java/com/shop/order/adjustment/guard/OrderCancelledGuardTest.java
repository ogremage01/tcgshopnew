package com.shop.order.adjustment.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shop.order.entity.OrderInfo;
import com.shop.order.enums.OrderStatus;

class OrderCancelledGuardTest {

    private final OrderCancelledGuard guard = new OrderCancelledGuard();

    @Test
    @DisplayName("취소되지 않은 주문은 통과한다")
    void passesWhenNotCancelled() {
        OrderInfo orderInfo = OrderInfo.builder()
                .orderStatus(OrderStatus.ORDER_PENDING.getValue())
                .build();

        assertThatCode(() -> guard.assertNotCancelled(orderInfo)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("취소된 주문은 409 ORDER_ALREADY_CANCELLED를 던진다")
    void throwsWhenCancelled() {
        OrderInfo orderInfo = OrderInfo.builder()
                .orderStatus(OrderStatus.ORDER_CANCELLED.getValue())
                .build();

        assertThatThrownBy(() -> guard.assertNotCancelled(orderInfo))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("ORDER_ALREADY_CANCELLED");
                });
    }
}
