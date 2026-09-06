package com.shop.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TossPaymentConfirmRequest {
    private String paymentKey;
    /** checkout draft publicId */
    private String orderId;
    private Long amount;
}
