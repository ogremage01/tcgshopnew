package com.shop.order.adjustment.step;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.adjustment.guard.OrderCancelledGuard;
import com.shop.order.entity.OrderInfo;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;

@ExtendWith(MockitoExtension.class)
class ValidateStepCancelReasonTest {

    @Mock
    private OrderInfoRepository orderInfoRepository;
    @Mock
    private OrderProductRepository orderProductRepository;
    @Mock
    private OrderCancelledGuard orderCancelledGuard;

    @InjectMocks
    private ValidateStep validateStep;

    @Test
    @DisplayName("FULL_CANCEL에서 cancelReason 누락 시 400")
    void requiresCancelReasonOnFullCancel() {
        when(orderInfoRepository.findById(1L)).thenReturn(Optional.of(OrderInfo.builder().id(1L).build()));

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderId(1L)
                .cancelReason("  ")
                .build();

        assertThatThrownBy(() -> validateStep.run(ctx))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    org.assertj.core.api.Assertions.assertThat(rse.getReason()).isEqualTo("CANCEL_REASON_REQUIRED");
                });
    }

    @Test
    @DisplayName("PARTIAL_MODIFY에서 cancelReason 누락 시 400")
    void requiresCancelReasonOnPartialModify() {
        when(orderInfoRepository.findById(1L)).thenReturn(Optional.of(OrderInfo.builder().id(1L).build()));

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderId(1L)
                .cancelReason(null)
                .build();

        assertThatThrownBy(() -> validateStep.run(ctx))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    org.assertj.core.api.Assertions.assertThat(rse.getReason()).isEqualTo("CANCEL_REASON_REQUIRED");
                });
    }
}
