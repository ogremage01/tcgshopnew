package com.shop.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

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

@ExtendWith(MockitoExtension.class)
class TossPaymentServiceImplTest {

    @Mock
    private CheckoutDraftRepository checkoutDraftRepository;

    @Mock
    private CheckoutConfirmService checkoutConfirmService;

    @Mock
    private CheckoutDraftValidationService checkoutDraftValidationService;

    @Mock
    private TossPaymentsApiClient tossPaymentsApiClient;

    @InjectMocks
    private TossPaymentServiceImpl tossPaymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("amount 불일치 시 400 AMOUNT_MISMATCH")
    void rejectsAmountMismatch() {
        CheckoutDraft draft = readyDraft();

        when(checkoutDraftRepository.findByPublicId("draft-public-id")).thenReturn(Optional.of(draft));
        when(checkoutConfirmService.validateDraft(eq("draft-public-id"), any()))
                .thenReturn(ResponseEntity.ok().build());

        TossPaymentConfirmRequest request = confirmRequest(100L);
        UserIdentity identity = UserIdentity.ofGuest("guest-1");

        assertThatThrownBy(() -> tossPaymentService.confirmPayment(request, identity))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(400);
                    assertThat(rse.getReason()).isEqualTo("AMOUNT_MISMATCH");
                });

        verify(tossPaymentsApiClient, never()).confirmPayment(any(), any(), any(Long.class));
        verify(tossPaymentsApiClient, never()).getPayment(any());
        verify(checkoutConfirmService, never()).finalizeAfterPayment(any(), any(), any());
    }

    @Test
    @DisplayName("승인 2xx 정상 경로에서는 결제 조회를 하지 않는다")
    void happyPathDoesNotInquire() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().put("method", "카드")));
        when(checkoutConfirmService.finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class)))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> response = tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(tossPaymentsApiClient, never()).getPayment(any());
        verify(tossPaymentsApiClient, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("주문 확정 실패 시 토스 결제 자동 취소")
    void cancelsTossPaymentWhenCheckoutConfirmFails() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().put("method", "카드")));

        CheckoutConfirmFailureResponse failureBody = CheckoutConfirmFailureResponse.builder()
                .code("OUT_OF_STOCK")
                .message("Checkout validation failed.")
                .failedItems(java.util.List.of())
                .build();
        ResponseEntity<?> conflictResponse = ResponseEntity.status(HttpStatus.CONFLICT).body(failureBody);
        doReturn(conflictResponse).when(checkoutConfirmService)
                .finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class));
        when(tossPaymentsApiClient.cancelPayment(eq("payment-key"), any(), any()))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));

        ResponseEntity<?> response = tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(tossPaymentsApiClient).cancelPayment(
                eq("payment-key"),
                eq("주문 확정 실패로 자동 취소 (OUT_OF_STOCK)"),
                eq("compensate-cancel:draft-public-id:payment-key"));
    }

    @Test
    @DisplayName("CheckoutConfirmConflictException 발생 시 토스 결제 자동 취소 후 예외 재전파")
    void cancelsTossPaymentWhenCheckoutThrowsConflict() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));
        when(checkoutConfirmService.finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class)))
                .thenThrow(new CheckoutConfirmConflictException(
                        CheckoutConfirmFailureResponse.builder()
                                .code("OUT_OF_STOCK")
                                .message("Concurrency conflict while reserving stock.")
                                .failedItems(java.util.List.of())
                                .build()));
        when(tossPaymentsApiClient.cancelPayment(eq("payment-key"), any(), any()))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));

        assertThatThrownBy(() -> tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1")))
                .isInstanceOf(CheckoutConfirmConflictException.class);

        verify(tossPaymentsApiClient).cancelPayment(
                eq("payment-key"),
                eq("주문 확정 실패로 자동 취소 (OUT_OF_STOCK)"),
                eq("compensate-cancel:draft-public-id:payment-key"));
    }

    @Test
    @DisplayName("결제 취소도 실패하면 500 CHECKOUT_FAILED_PAYMENT_CANCEL_FAILED")
    void throwsWhenCancelAlsoFails() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode()));

        CheckoutConfirmFailureResponse failureBody = CheckoutConfirmFailureResponse.builder()
                .code("OUT_OF_STOCK")
                .message("failed")
                .failedItems(java.util.List.of())
                .build();
        ResponseEntity<?> conflictResponse = ResponseEntity.status(HttpStatus.CONFLICT).body(failureBody);
        doReturn(conflictResponse).when(checkoutConfirmService)
                .finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class));

        var cancelError = objectMapper.createObjectNode().put("code", "ALREADY_CANCELED");
        when(tossPaymentsApiClient.cancelPayment(eq("payment-key"), any(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cancelError));

        assertThatThrownBy(() -> tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(500);
                    assertThat(rse.getReason()).isEqualTo("CHECKOUT_FAILED_PAYMENT_CANCEL_FAILED");
                });
    }

    @Test
    @DisplayName("승인 타임아웃 후 조회가 DONE이면 주문을 확정하고 취소하지 않는다")
    void inquiresAndFinalizesWhenConfirmTimesOutAndPaymentIsDone() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenThrow(new TossPaymentsUncertainStateException("confirm", "draft-public-id", null));
        when(tossPaymentsApiClient.getPayment("payment-key"))
                .thenReturn(ResponseEntity.ok(
                        objectMapper.createObjectNode().put("status", "DONE").put("method", "카드")));
        when(checkoutConfirmService.finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class)))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> response = tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(checkoutConfirmService).finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class));
        verify(tossPaymentsApiClient, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("승인 타임아웃 후 IN_PROGRESS이면 같은 결제키로 승인을 한 번 재시도한다")
    void retriesConfirmWhenInquiryShowsInProgress() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenThrow(new TossPaymentsUncertainStateException("confirm", "draft-public-id", null))
                .thenReturn(ResponseEntity.ok(
                        objectMapper.createObjectNode().put("status", "DONE").put("method", "카드")));
        when(tossPaymentsApiClient.getPayment("payment-key"))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().put("status", "IN_PROGRESS")));
        when(checkoutConfirmService.finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class)))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> response = tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(tossPaymentsApiClient, times(2)).confirmPayment("payment-key", "draft-public-id", 50000L);
        verify(tossPaymentsApiClient, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("승인 타임아웃 후 EXPIRED이면 주문을 만들지 않는다")
    void doesNotFinalizeWhenInquiryShowsExpired() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenThrow(new TossPaymentsUncertainStateException("confirm", "draft-public-id", null));
        when(tossPaymentsApiClient.getPayment("payment-key"))
                .thenReturn(ResponseEntity.ok(objectMapper.createObjectNode().put("status", "EXPIRED")));

        ResponseEntity<?> response = tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(checkoutConfirmService, never()).finalizeAfterPayment(any(), any(), any());
        verify(tossPaymentsApiClient, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("승인·조회가 모두 불확정이면 취소하지 않고 503 PAYMENT_STATUS_UNKNOWN")
    void doesNotCancelWhenConfirmAndInquiryAreUncertain() {
        stubReadyDraft();
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenThrow(new TossPaymentsUncertainStateException("confirm", "draft-public-id", null));
        when(tossPaymentsApiClient.getPayment("payment-key"))
                .thenThrow(new TossPaymentsUncertainStateException("getPayment", "payment-key", null));

        assertThatThrownBy(() -> tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(503);
                    assertThat(rse.getReason()).isEqualTo("PAYMENT_STATUS_UNKNOWN");
                });

        verify(checkoutConfirmService, never()).finalizeAfterPayment(any(), any(), any());
        verify(tossPaymentsApiClient, never()).cancelPayment(any(), any(), any());
    }

    @Test
    @DisplayName("ALREADY_PROCESSED_PAYMENT이면 조회 후 DONE이면 주문을 확정한다")
    void treatsAlreadyProcessedAsSuccessWhenInquiryIsDone() {
        stubReadyDraft();
        var alreadyProcessed = objectMapper.createObjectNode().put("code", "ALREADY_PROCESSED_PAYMENT");
        when(tossPaymentsApiClient.confirmPayment("payment-key", "draft-public-id", 50000L))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(alreadyProcessed));
        when(tossPaymentsApiClient.getPayment("payment-key"))
                .thenReturn(ResponseEntity.ok(
                        objectMapper.createObjectNode().put("status", "DONE").put("method", "카드")));
        when(checkoutConfirmService.finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class)))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> response = tossPaymentService.confirmPayment(
                confirmRequest(50000L), UserIdentity.ofGuest("guest-1"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(checkoutConfirmService).finalizeAfterPayment(eq("draft-public-id"), any(), any(CheckoutConfirmContext.class));
        verify(tossPaymentsApiClient, never()).cancelPayment(any(), any(), any());
    }

    private void stubReadyDraft() {
        when(checkoutDraftRepository.findByPublicId("draft-public-id")).thenReturn(Optional.of(readyDraft()));
        when(checkoutConfirmService.validateDraft(eq("draft-public-id"), any()))
                .thenReturn(ResponseEntity.ok().build());
    }

    private static CheckoutDraft readyDraft() {
        return CheckoutDraft.builder()
                .publicId("draft-public-id")
                .guestId("guest-1")
                .status(DraftStatus.READY)
                .totalAmount(new BigDecimal("50000"))
                .build();
    }

    private static TossPaymentConfirmRequest confirmRequest(long amount) {
        return TossPaymentConfirmRequest.builder()
                .paymentKey("payment-key")
                .orderId("draft-public-id")
                .amount(amount)
                .build();
    }
}
