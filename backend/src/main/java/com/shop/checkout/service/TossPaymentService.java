package com.shop.checkout.service;

import org.springframework.http.ResponseEntity;

import com.shop.checkout.dto.TossPaymentConfirmRequest;
import com.shop.common.identity.UserIdentity;

public interface TossPaymentService {

    ResponseEntity<?> confirmPayment(TossPaymentConfirmRequest request, UserIdentity identity);
}
