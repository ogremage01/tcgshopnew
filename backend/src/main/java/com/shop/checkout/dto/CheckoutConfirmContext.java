package com.shop.checkout.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CheckoutConfirmContext {
    String paymentStatus;
    String orderStatus;
    String pgTransactionId;
    String paymentMethod;
}
