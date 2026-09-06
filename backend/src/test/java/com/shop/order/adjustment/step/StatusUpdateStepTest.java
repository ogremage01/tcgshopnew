package com.shop.order.adjustment.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.entity.OrderInfo;
import com.shop.order.enums.OrderStatus;
import com.shop.order.payment.OrderPaymentStatuses;
import com.shop.order.repository.OrderInfoRepository;

@ExtendWith(MockitoExtension.class)
class StatusUpdateStepTest {

    @Mock
    private OrderInfoRepository orderInfoRepository;

    @InjectMocks
    private StatusUpdateStep statusUpdateStep;

    @Test
    @DisplayName("FULL_CANCEL 시 ORDER_CANCELLED와 PAYMENT_CANCELLED를 설정한다")
    void fullCancelSetsPaymentCancelled() {
        OrderInfo order = OrderInfo.builder()
                .id(1L)
                .orderStatus(OrderStatus.ORDER_COMPLETED.getValue())
                .paymentStatus(OrderPaymentStatuses.PAYMENT_COMPLETED)
                .build();
        when(orderInfoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderInfo(order)
                .build();

        statusUpdateStep.run(ctx);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ORDER_CANCELLED.getValue());
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatuses.PAYMENT_CANCELLED);
        assertThat(ctx.isOrderCancelled()).isTrue();
        verify(orderInfoRepository).save(order);
    }

    @Test
    @DisplayName("DIRECT 결제는 paymentStatus를 PAYMENT_CANCELLED로 바꾸지 않는다")
    void keepsDirectPaymentStatus() {
        OrderInfo order = OrderInfo.builder()
                .id(1L)
                .orderStatus(OrderStatus.ORDER_PENDING.getValue())
                .paymentStatus(OrderPaymentStatuses.PAYMENT_DIRECT)
                .build();
        when(orderInfoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderInfo(order)
                .build();

        statusUpdateStep.run(ctx);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ORDER_CANCELLED.getValue());
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatuses.PAYMENT_DIRECT);
    }
}
