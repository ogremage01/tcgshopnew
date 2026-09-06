package com.shop.checkout.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.checkout.dto.CheckoutDraftCreateResponse;
import com.shop.checkout.dto.CheckoutDraftResponse;
import com.shop.checkout.dto.PatchCheckoutDraftRequest;
import com.shop.checkout.service.CheckoutConfirmService;
import com.shop.checkout.service.CheckoutDraftService;
import com.shop.checkout.service.CheckoutDraftValidationService;
import com.shop.common.identity.IdentityResolver;
import com.shop.order.dto.OrderConfigDto;
import com.shop.admin.order.service.AdminOrderService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final IdentityResolver identityResolver;
    private final CheckoutDraftService checkoutDraftService;
    private final CheckoutConfirmService checkoutConfirmService;
    private final CheckoutDraftValidationService checkoutDraftValidationService;
    private final AdminOrderService adminOrderService;
    // 주문 초안 생성
    @PostMapping("/drafts")
    public CheckoutDraftCreateResponse createDraft(HttpServletRequest request, Authentication authentication) {
        return checkoutDraftService.createDraft(identityResolver.resolve(request, authentication));
    }

    // 주문 초안 조회
    @GetMapping("/drafts/{publicId}")
    public CheckoutDraftResponse getDraft(
            HttpServletRequest request,
            Authentication authentication,
            @PathVariable String publicId) {
        return checkoutDraftService.getDraft(publicId, identityResolver.resolve(request, authentication));
    }

    // 주문 초안 수정
    @PatchMapping("/drafts/{publicId}")
    public CheckoutDraftResponse patchDraft(
            HttpServletRequest request,
            Authentication authentication,
            @PathVariable String publicId,
            @RequestBody PatchCheckoutDraftRequest body) {
        return checkoutDraftService.patchDraft(publicId, body, identityResolver.resolve(request, authentication));
    }

    // 주문 초안 검증 (토스 결제 전 등)
    @PostMapping("/drafts/{publicId}/validate")
    public ResponseEntity<?> validateDraft(
            HttpServletRequest request,
            Authentication authentication,
            @PathVariable String publicId) {
        return checkoutDraftValidationService.validateDraft(
                publicId, identityResolver.resolve(request, authentication));
    }

    // 주문 초안 확정 (매장 직접결제)
    @PostMapping("/drafts/{publicId}/confirm")
    public ResponseEntity<?> confirmDraft(
            HttpServletRequest request,
            Authentication authentication,
            @PathVariable String publicId) {
        return checkoutConfirmService.confirm(publicId, identityResolver.resolve(request, authentication));
    }

    //===============================================
    // 배송비/결제 통화 설정 조회
    //===============================================
    @GetMapping("/config")
    public ResponseEntity<List<OrderConfigDto>> getConfigList() {
        return ResponseEntity.ok(adminOrderService.getConfigList());
    }
}
