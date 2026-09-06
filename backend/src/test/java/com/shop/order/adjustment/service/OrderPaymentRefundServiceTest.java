package com.shop.order.adjustment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shop.checkout.client.TossPaymentsApiClient;
import com.shop.order.entity.OrderInfo;
import com.shop.order.payment.OrderPaymentStatuses;

@ExtendWith(MockitoExtension.class)
class OrderPaymentRefundServiceTest {

    @Mock
    private TossPaymentsApiClient tossPaymentsApiClient;

    @InjectMocks
    private OrderPaymentRefundService orderPaymentRefundService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("DIRECT 결제는 Toss 호출 없이 성공")
    void skipsDirectPayment() {
        OrderInfo order = paidOrder();
        order.setPaymentMethod("DIRECT");

        assertThat(orderPaymentRefundService.refund(order, new BigDecimal("10000"), "사유", null))
                .isTrue();
    }

    @Test
    @DisplayName("Toss 전액 취소 성공 (cancelAmount null)")
    void refundSuccess() {
        OrderInfo order = paidOrder();
        when(tossPaymentsApiClient.cancelPayment(
                        eq("pk-1"), eq("고객 변심"), isNull(), eq("full-cancel-10")))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));

        orderPaymentRefundService.refundOrThrow(order, new BigDecimal("10000"), "고객 변심");

        verify(tossPaymentsApiClient)
                .cancelPayment("pk-1", "고객 변심", null, "full-cancel-10");
    }

    @Test
    @DisplayName("ALREADY_CANCELED_PAYMENT는 성공으로 처리")
    void alreadyCanceledIsSuccess() {
        OrderInfo order = paidOrder();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", "ALREADY_CANCELED_PAYMENT");
        when(tossPaymentsApiClient.cancelPayment(
                        eq("pk-1"), eq("재시도"), isNull(), eq("full-cancel-10")))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body));

        orderPaymentRefundService.refundOrThrow(order, new BigDecimal("10000"), "재시도");
    }

    @Test
    @DisplayName("Toss 실패 시 409 TOSS_REFUND_FAILED")
    void refundFailureThrowsConflict() {
        OrderInfo order = paidOrder();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", "FAILED_ERROR");
        when(tossPaymentsApiClient.cancelPayment(
                        eq("pk-1"), eq("사유"), isNull(), eq("full-cancel-10")))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body));

        assertThatThrownBy(() -> orderPaymentRefundService.refundOrThrow(order, new BigDecimal("10000"), "사유"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("TOSS_REFUND_FAILED");
                });
    }

    @Test
    @DisplayName("cancelReason 없으면 soft refund는 false")
    void blankReasonReturnsFalse() {
        OrderInfo order = paidOrder();
        assertThat(orderPaymentRefundService.refund(order, new BigDecimal("10000"), "  ", null))
                .isFalse();
    }

    @Test
    @DisplayName("부분 환불은 cancelAmount와 partial-cancel 멱등키를 전달한다")
    void partialRefundPassesCancelAmount() {
        OrderInfo order = paidOrder();
        when(tossPaymentsApiClient.cancelPayment(
                        eq("pk-1"),
                        eq("부분"),
                        eq(new BigDecimal("1000")),
                        startsWith("partial-cancel-10-")))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));

        orderPaymentRefundService.refundPartialOrThrow(order, new BigDecimal("1000"), "부분");

        verify(tossPaymentsApiClient)
                .cancelPayment(
                        eq("pk-1"),
                        eq("부분"),
                        eq(new BigDecimal("1000")),
                        startsWith("partial-cancel-10-"));
    }

    @Test
    @DisplayName("부분 환불 사유 없으면 400 CANCEL_REASON_REQUIRED")
    void partialBlankReasonThrows() {
        OrderInfo order = paidOrder();
        assertThatThrownBy(
                        () -> orderPaymentRefundService.refundPartialOrThrow(
                                order, new BigDecimal("1000"), " "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("CANCEL_REASON_REQUIRED");
                });
    }

    @Test
    @DisplayName("부분 환불 실패 시 409 TOSS_REFUND_FAILED")
    void partialRefundFailureThrowsConflict() {
        OrderInfo order = paidOrder();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("code", "FAILED_ERROR");
        when(tossPaymentsApiClient.cancelPayment(any(), any(), any(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body));

        assertThatThrownBy(
                        () -> orderPaymentRefundService.refundPartialOrThrow(
                                order, new BigDecimal("1000"), "사유"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("TOSS_REFUND_FAILED");
                });
    }

    private static OrderInfo paidOrder() {
        return OrderInfo.builder()
                .id(10L)
                .paymentStatus(OrderPaymentStatuses.PAYMENT_COMPLETED)
                .paymentMethod("카드")
                .pgTransactionId("pk-1")
                .actualPaymentAmount(new BigDecimal("10000"))
                .build();
    }
}
