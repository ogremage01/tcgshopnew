package com.shop.order.adjustment.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.shop.checkout.client.TossPaymentsApiClient;
import com.shop.order.entity.OrderInfo;
import com.shop.order.payment.OrderPaymentStatuses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentRefundService {

    private static final String ALREADY_CANCELED_PAYMENT = "ALREADY_CANCELED_PAYMENT";
    private static final String DIRECT_PAYMENT_METHOD = "DIRECT";

    private final TossPaymentsApiClient tossPaymentsApiClient;

    /**
     * PG사 환불 API 연동.
     *
     * @param cancelAmount null이면 Toss 전액 취소, 값이 있으면 해당 금액 부분 취소
     * @return true if refund succeeded (or already canceled / skipped)
     */
    public boolean refund(
            OrderInfo orderInfo,
            BigDecimal cancelAmount,
            String cancelReason,
            String idempotencyKey) {
        if (!requiresTossRefund(orderInfo, cancelAmount)) {
            return true;
        }
        if (cancelReason == null || cancelReason.isBlank()) {
            return false;
        }

        String paymentKey = orderInfo.getPgTransactionId();
        ResponseEntity<JsonNode> result = tossPaymentsApiClient.cancelPayment(
                paymentKey,
                cancelReason.trim(),
                cancelAmount,
                idempotencyKey);

        if (result.getStatusCode().is2xxSuccessful()) {
            log.info(
                    "Toss refund succeeded orderId={} paymentKey={} cancelAmount={} idempotencyKey={}",
                    orderInfo.getId(),
                    paymentKey,
                    cancelAmount,
                    idempotencyKey);
            return true;
        }

        if (isAlreadyCanceled(result.getBody())) {
            log.info(
                    "Toss payment already canceled orderId={} paymentKey={}",
                    orderInfo.getId(),
                    paymentKey);
            return true;
        }

        String tossCode = extractTossCode(result.getBody());
        log.warn(
                "Toss refund failed orderId={} paymentKey={} status={} code={} body={}",
                orderInfo.getId(),
                paymentKey,
                result.getStatusCode().value(),
                tossCode,
                result.getBody());
        return false;
    }

    /** FULL_CANCEL용. cancelAmount null → Toss 전액 취소. 실패 시 409 TOSS_REFUND_FAILED. */
    public void refundOrThrow(OrderInfo orderInfo, BigDecimal refundAmount, String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANCEL_REASON_REQUIRED");
        }
        String idempotencyKey = "full-cancel-" + orderInfo.getId();
        boolean success = refund(orderInfo, null, cancelReason, idempotencyKey);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "TOSS_REFUND_FAILED");
        }
    }

    /**
     * PARTIAL_MODIFY용. 재계산된 차액을 cancelAmount로 전달. 실패 시 409 TOSS_REFUND_FAILED.
     */
    public void refundPartialOrThrow(OrderInfo orderInfo, BigDecimal cancelAmount, String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CANCEL_REASON_REQUIRED");
        }
        String idempotencyKey = "partial-cancel-" + orderInfo.getId() + "-" + UUID.randomUUID();
        boolean success = refund(orderInfo, cancelAmount, cancelReason, idempotencyKey);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "TOSS_REFUND_FAILED");
        }
    }

    public static boolean requiresTossRefund(OrderInfo orderInfo, BigDecimal refundAmount) {
        if (orderInfo == null) {
            return false;
        }
        if (DIRECT_PAYMENT_METHOD.equals(orderInfo.getPaymentMethod())) {
            return false;
        }
        if (!OrderPaymentStatuses.PAYMENT_COMPLETED.equals(orderInfo.getPaymentStatus())) {
            return false;
        }
        String paymentKey = orderInfo.getPgTransactionId();
        if (paymentKey == null || paymentKey.isBlank()) {
            return false;
        }
        // FULL_CANCEL: refundAmount may be original amount while cancelAmount passed as null to Toss.
        // Gate on refundAmount when provided; for full cancel refundOrThrow always has amount > 0 from step.
        return refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean isAlreadyCanceled(JsonNode body) {
        return ALREADY_CANCELED_PAYMENT.equals(extractTossCode(body));
    }

    private static String extractTossCode(JsonNode body) {
        if (body == null || body.isNull()) {
            return null;
        }
        JsonNode code = body.get("code");
        if (code != null && !code.isNull()) {
            return code.asText();
        }
        return null;
    }
}
