package com.shop.checkout.service;

import java.math.RoundingMode;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.shop.checkout.client.TossPaymentsApiClient;
import com.shop.checkout.domain.DraftStatus;
import com.shop.checkout.dto.CheckoutConfirmContext;
import com.shop.checkout.dto.CheckoutConfirmFailureResponse;
import com.shop.checkout.dto.TossPaymentConfirmRequest;
import com.shop.checkout.entity.CheckoutDraft;
import com.shop.checkout.exception.CheckoutConfirmConflictException;
import com.shop.checkout.exception.TossPaymentsUncertainStateException;
import com.shop.checkout.repository.CheckoutDraftRepository;
import com.shop.common.identity.UserIdentity;
import com.shop.order.enums.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TossPaymentServiceImpl implements TossPaymentService {

    private static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String CANCEL_REASON = "주문 확정 실패로 자동 취소";
    private static final String ALREADY_CANCELED_PAYMENT = "ALREADY_CANCELED_PAYMENT";
    private static final Set<String> ALREADY_PROCESSED_CODES = Set.of(
            "ALREADY_PROCESSED_PAYMENT",
            "ALREADY_COMPLETED_PAYMENT");
    private static final Set<String> APPROVED_TOSS_STATUSES = Set.of("DONE", "WAITING_FOR_DEPOSIT");
    private static final Set<String> RETRYABLE_TOSS_STATUSES = Set.of("IN_PROGRESS", "READY");

    private final CheckoutDraftRepository checkoutDraftRepository;
    private final CheckoutConfirmService checkoutConfirmService;
    private final CheckoutDraftValidationService checkoutDraftValidationService;
    private final TossPaymentsApiClient tossPaymentsApiClient;

    @Override
    public ResponseEntity<?> confirmPayment(TossPaymentConfirmRequest request, UserIdentity identity) {
        validateRequest(request);

        log.info("Toss confirm started orderId={} isUser={} guestId={}",
                request.getOrderId(), identity.isUser(),
                identity.isUser() ? null : identity.getGuestId());

        CheckoutDraft draft = checkoutDraftRepository.findByPublicId(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRAFT_NOT_FOUND"));

        checkoutDraftValidationService.assertOwner(draft, identity);

        if (draft.getStatus() == DraftStatus.CONFIRMED) {
            return checkoutConfirmService.finalizeAfterPayment(request.getOrderId(), identity, null);
        }

        ResponseEntity<?> preValidation = checkoutConfirmService.validateDraft(request.getOrderId(), identity);
        if (!preValidation.getStatusCode().is2xxSuccessful()) {
            return preValidation;
        }

        verifyAmount(draft, request.getAmount());

        log.info("Calling Toss confirm API orderId={} amount={}", request.getOrderId(), request.getAmount());
        ResponseEntity<JsonNode> tossResult = confirmWithRecovery(request);
        if (!tossResult.getStatusCode().is2xxSuccessful()) {
            log.warn("Toss confirm API returned non-2xx orderId={} status={}",
                    request.getOrderId(), tossResult.getStatusCode().value());
            return tossResult;
        }

        JsonNode tossResponse = tossResult.getBody();
        CheckoutConfirmContext context = CheckoutConfirmContext.builder()
                .paymentStatus(PAYMENT_COMPLETED)
                .orderStatus(OrderStatus.ORDER_COMPLETED.getValue())
                .pgTransactionId(request.getPaymentKey())
                .paymentMethod(extractPaymentMethod(tossResponse))
                .build();

        return finalizeWithCompensation(request, identity, context);
    }

    private ResponseEntity<JsonNode> confirmWithRecovery(TossPaymentConfirmRequest request) {
        try {
            ResponseEntity<JsonNode> result = tossPaymentsApiClient.confirmPayment(
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount());
            if (isAlreadyProcessed(result)) {
                log.info(
                        "Toss confirm already processed, inquiring paymentKey={} orderId={}",
                        request.getPaymentKey(),
                        request.getOrderId());
                return resolveByInquiry(request, true);
            }
            return result;
        } catch (TossPaymentsUncertainStateException ex) {
            log.warn(
                    "Toss confirm uncertain, inquiring paymentKey={} orderId={}",
                    request.getPaymentKey(),
                    request.getOrderId(),
                    ex);
            return resolveByInquiry(request, true);
        }
    }

    private ResponseEntity<JsonNode> resolveByInquiry(
            TossPaymentConfirmRequest request, boolean retryConfirmIfInProgress) {
        ResponseEntity<JsonNode> inquired;
        try {
            inquired = tossPaymentsApiClient.getPayment(request.getPaymentKey());
        } catch (TossPaymentsUncertainStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_STATUS_UNKNOWN");
        }

        if (inquired.getStatusCode().is2xxSuccessful()) {
            String status = extractStatus(inquired.getBody());
            if (APPROVED_TOSS_STATUSES.contains(status)) {
                return inquired;
            }
            if (retryConfirmIfInProgress && RETRYABLE_TOSS_STATUSES.contains(status)) {
                try {
                    ResponseEntity<JsonNode> retried = tossPaymentsApiClient.confirmPayment(
                            request.getPaymentKey(),
                            request.getOrderId(),
                            request.getAmount());
                    if (isAlreadyProcessed(retried)) {
                        return resolveByInquiry(request, false);
                    }
                    return retried;
                } catch (TossPaymentsUncertainStateException ex) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_STATUS_UNKNOWN");
                }
            }
            log.warn(
                    "Toss payment not confirmable status={} orderId={} paymentKey={}",
                    status,
                    request.getOrderId(),
                    request.getPaymentKey());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(inquired.getBody());
        }
        return inquired;
    }

    private ResponseEntity<?> finalizeWithCompensation(
            TossPaymentConfirmRequest request,
            UserIdentity identity,
            CheckoutConfirmContext context) {
        try {
            ResponseEntity<?> confirmResult = checkoutConfirmService.finalizeAfterPayment(
                    request.getOrderId(),
                    identity,
                    context);
            if (!confirmResult.getStatusCode().is2xxSuccessful()) {
                compensateTossPayment(request.getPaymentKey(), request.getOrderId(), describeFailure(confirmResult));
            }
            return confirmResult;
        } catch (CheckoutConfirmConflictException ex) {
            compensateTossPayment(request.getPaymentKey(), request.getOrderId(), ex.getBody().getCode());
            throw ex;
        } catch (ResponseStatusException ex) {
            compensateTossPayment(request.getPaymentKey(), request.getOrderId(), ex.getReason());
            throw ex;
        } catch (RuntimeException ex) {
            compensateTossPayment(request.getPaymentKey(), request.getOrderId(), ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private void compensateTossPayment(String paymentKey, String orderId, String reason) {
        String cancelReason = CANCEL_REASON + (reason != null ? " (" + reason + ")" : "");
        String idempotencyKey = TossPaymentsApiClient.compensateCancelIdempotencyKey(orderId, paymentKey);
        ResponseEntity<JsonNode> cancelResult;
        try {
            cancelResult = tossPaymentsApiClient.cancelPayment(paymentKey, cancelReason, idempotencyKey);
        } catch (TossPaymentsUncertainStateException ex) {
            log.warn("Toss compensate cancel uncertain paymentKey={} orderId={}", paymentKey, orderId, ex);
            cancelResult = resolveCancelByInquiry(paymentKey, orderId, cancelReason, idempotencyKey);
        }
        if (isSuccessfulCancel(cancelResult)) {
            log.info(
                    "Toss payment cancelled after checkout failure paymentKey={} orderId={} reason={}",
                    paymentKey,
                    orderId,
                    reason);
            return;
        }
        log.error(
                "CRITICAL: checkout failed and Toss cancel also failed paymentKey={} orderId={} cancelStatus={} body={}",
                paymentKey,
                orderId,
                cancelResult != null ? cancelResult.getStatusCode().value() : null,
                cancelResult != null ? cancelResult.getBody() : null);
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "CHECKOUT_FAILED_PAYMENT_CANCEL_FAILED");
    }

    private ResponseEntity<JsonNode> resolveCancelByInquiry(
            String paymentKey, String orderId, String cancelReason, String idempotencyKey) {
        ResponseEntity<JsonNode> inquired;
        try {
            inquired = tossPaymentsApiClient.getPayment(paymentKey);
        } catch (TossPaymentsUncertainStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CHECKOUT_FAILED_PAYMENT_CANCEL_FAILED");
        }
        if (inquired.getStatusCode().is2xxSuccessful()) {
            String status = extractStatus(inquired.getBody());
            if ("CANCELED".equals(status) || "PARTIAL_CANCELED".equals(status)) {
                return inquired;
            }
            if (APPROVED_TOSS_STATUSES.contains(status)) {
                try {
                    return tossPaymentsApiClient.cancelPayment(paymentKey, cancelReason, idempotencyKey);
                } catch (TossPaymentsUncertainStateException ex) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "CHECKOUT_FAILED_PAYMENT_CANCEL_FAILED");
                }
            }
        }
        return inquired;
    }

    private static boolean isSuccessfulCancel(ResponseEntity<JsonNode> cancelResult) {
        if (cancelResult == null) {
            return false;
        }
        if (cancelResult.getStatusCode().is2xxSuccessful()) {
            return true;
        }
        return ALREADY_CANCELED_PAYMENT.equals(extractCode(cancelResult.getBody()));
    }

    private static boolean isAlreadyProcessed(ResponseEntity<JsonNode> result) {
        if (result == null || result.getStatusCode().is2xxSuccessful()) {
            return false;
        }
        return ALREADY_PROCESSED_CODES.contains(extractCode(result.getBody()));
    }

    private static String describeFailure(ResponseEntity<?> confirmResult) {
        Object body = confirmResult.getBody();
        if (body instanceof CheckoutConfirmFailureResponse failure) {
            return failure.getCode();
        }
        return String.valueOf(confirmResult.getStatusCode().value());
    }

    private void validateRequest(TossPaymentConfirmRequest request) {
        if (request == null
                || isBlank(request.getPaymentKey())
                || isBlank(request.getOrderId())
                || request.getAmount() == null
                || request.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_TOSS_CONFIRM_REQUEST");
        }
    }

    private void verifyAmount(CheckoutDraft draft, long requestedAmount) {
        if (draft.getTotalAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AMOUNT_MISMATCH");
        }
        long expectedAmount = draft.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        if (expectedAmount != requestedAmount) {
            log.warn(
                    "Toss amount mismatch draftPublicId={} expected={} requested={}",
                    draft.getPublicId(),
                    expectedAmount,
                    requestedAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AMOUNT_MISMATCH");
        }
    }

    private static String extractPaymentMethod(JsonNode tossResponse) {
        if (tossResponse == null) {
            return "TOSS";
        }
        JsonNode method = tossResponse.get("method");
        if (method != null && !method.isNull() && !method.asText().isBlank()) {
            return method.asText();
        }
        return "TOSS";
    }

    private static String extractStatus(JsonNode tossResponse) {
        if (tossResponse == null) {
            return "";
        }
        JsonNode status = tossResponse.get("status");
        if (status != null && !status.isNull()) {
            return status.asText();
        }
        return "";
    }

    private static String extractCode(JsonNode body) {
        if (body == null || body.isNull()) {
            return null;
        }
        JsonNode code = body.get("code");
        if (code != null && !code.isNull()) {
            return code.asText();
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
