package com.shop.checkout.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.checkout.dto.TossPaymentConfirmRequest;
import com.shop.checkout.service.TossPaymentService;
import com.shop.common.identity.IdentityResolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/checkout/toss")
@RequiredArgsConstructor
public class TossPaymentController {

    private final TossPaymentService tossPaymentService;
    private final IdentityResolver identityResolver;

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            HttpServletRequest request,
            Authentication authentication,
            @RequestBody TossPaymentConfirmRequest body) {
        return tossPaymentService.confirmPayment(body, identityResolver.resolve(request, authentication));
    }
}
