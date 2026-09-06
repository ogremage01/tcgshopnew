package com.shop.order.adjustment.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

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
import com.shop.order.adjustment.service.OrderPaymentRefundService;
import com.shop.order.entity.OrderInfo;
import com.shop.order.payment.OrderPaymentStatuses;

@ExtendWith(MockitoExtension.class)
class PaymentRefundStepTest {

    @Mock
    private OrderPaymentRefundService orderPaymentRefundService;

    @InjectMocks
    private PaymentRefundStep paymentRefundStep;

    @Test
    @DisplayName("FULL_CANCEL에서 Toss 실패 시 예외를 그대로 전파한다")
    void fullCancelPropagatesRefundFailure() {
        OrderInfo order = OrderInfo.builder()
                .id(1L)
                .paymentStatus(OrderPaymentStatuses.PAYMENT_COMPLETED)
                .paymentMethod("카드")
                .pgTransactionId("pk")
                .build();
        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderInfo(order)
                .originalActualPaymentAmount(new BigDecimal("5000"))
                .cancelReason("고객 요청")
                .build();

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "TOSS_REFUND_FAILED"))
                .when(orderPaymentRefundService)
                .refundOrThrow(eq(order), eq(new BigDecimal("5000")), eq("고객 요청"));

        assertThatThrownBy(() -> paymentRefundStep.run(ctx))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason())
                        .isEqualTo("TOSS_REFUND_FAILED"));
    }

    @Test
    @DisplayName("DIRECT 결제는 환불 서비스를 호출하지 않는다")
    void skipsDirect() {
        OrderInfo order = OrderInfo.builder()
                .id(1L)
                .paymentStatus(OrderPaymentStatuses.PAYMENT_DIRECT)
                .paymentMethod("DIRECT")
                .actualPaymentAmount(new BigDecimal("5000"))
                .build();
        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderInfo(order)
                .originalActualPaymentAmount(new BigDecimal("5000"))
                .cancelReason("사유")
                .build();

        paymentRefundStep.run(ctx);

        verifyNoInteractions(orderPaymentRefundService);
        assertThat(ctx.isPgRefundSuccess()).isTrue();
    }

    @Test
    @DisplayName("PARTIAL_MODIFY는 refundPartialOrThrow로 hard-fail한다")
    void partialHardFails() {
        OrderInfo order = OrderInfo.builder()
                .id(1L)
                .paymentStatus(OrderPaymentStatuses.PAYMENT_COMPLETED)
                .paymentMethod("카드")
                .pgTransactionId("pk")
                .build();
        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderInfo(order)
                .pgRefundAmount(new BigDecimal("1000"))
                .cancelReason("부분 취소")
                .build();

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "TOSS_REFUND_FAILED"))
                .when(orderPaymentRefundService)
                .refundPartialOrThrow(eq(order), eq(new BigDecimal("1000")), eq("부분 취소"));

        assertThatThrownBy(() -> paymentRefundStep.run(ctx))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason())
                        .isEqualTo("TOSS_REFUND_FAILED"));

        verify(orderPaymentRefundService, never()).refundOrThrow(any(), any(), any());
        verify(orderPaymentRefundService)
                .refundPartialOrThrow(eq(order), eq(new BigDecimal("1000")), eq("부분 취소"));
    }

    @Test
    @DisplayName("PARTIAL_MODIFY 성공 시 pgRefundSuccess true")
    void partialSuccess() {
        OrderInfo order = OrderInfo.builder()
                .id(1L)
                .paymentStatus(OrderPaymentStatuses.PAYMENT_COMPLETED)
                .paymentMethod("카드")
                .pgTransactionId("pk")
                .build();
        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderInfo(order)
                .pgRefundAmount(new BigDecimal("1000"))
                .cancelReason("부분 취소")
                .build();

        paymentRefundStep.run(ctx);

        verify(orderPaymentRefundService)
                .refundPartialOrThrow(eq(order), eq(new BigDecimal("1000")), eq("부분 취소"));
        assertThat(ctx.isPgRefundSuccess()).isTrue();
    }
}
