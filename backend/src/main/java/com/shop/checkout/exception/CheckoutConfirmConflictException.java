package com.shop.checkout.exception;

import com.shop.checkout.dto.CheckoutConfirmFailureResponse;

import lombok.Getter;

@Getter
public class CheckoutConfirmConflictException extends RuntimeException {

    private final CheckoutConfirmFailureResponse body;

    public CheckoutConfirmConflictException(CheckoutConfirmFailureResponse body) {
        super(body != null ? body.getCode() : "CHECKOUT_CONFLICT");
        this.body = body;
    }
}
