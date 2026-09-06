package com.shop.checkout.service;

import java.util.Optional;

import org.springframework.http.ResponseEntity;

import com.shop.checkout.entity.CheckoutDraft;
import com.shop.common.identity.UserIdentity;

public interface CheckoutDraftValidationService {

    void assertOwner(CheckoutDraft draft, UserIdentity identity);

    /**
     * READY 드래프트 검증. 성공 시 empty, 실패 시 HTTP 응답(본문·상태코드 포함).
     * 호출 전 CONFIRMED 멱등은 오케스트레이터에서 처리한다.
     */
    Optional<ResponseEntity<?>> validateReadyDraft(CheckoutDraft draft, UserIdentity identity);

    /**
     * 결제 전 API용. publicId로 draft 로드(락 없음) 후 검증.
     */
    ResponseEntity<?> validateDraft(String draftPublicId, UserIdentity identity);
}
