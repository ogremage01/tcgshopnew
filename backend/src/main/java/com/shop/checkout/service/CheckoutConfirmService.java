package com.shop.checkout.service;

import org.springframework.http.ResponseEntity;

import com.shop.checkout.dto.CheckoutConfirmContext;
import com.shop.common.identity.UserIdentity;

public interface CheckoutConfirmService {

    /**
     * 매장 직접결제 등 기본 확정 (PAYMENT_DIRECT / ORDER_PENDING).
     */
    ResponseEntity<?> confirm(String draftPublicId, UserIdentity identity);

    /**
     * 토스 결제 완료 후 확정. paymentContext로 결제·주문 상태를 지정한다.
     * context가 null이면 {@link #confirm}과 동일하게 직접결제 기본값을 사용한다.
     */
    ResponseEntity<?> finalizeAfterPayment(String draftPublicId, UserIdentity identity, CheckoutConfirmContext context);

    /**
     * 결제 전 드래프트 검증 (토스 결제 전·프론트 validate API).
     */
    ResponseEntity<?> validateDraft(String draftPublicId, UserIdentity identity);
}
