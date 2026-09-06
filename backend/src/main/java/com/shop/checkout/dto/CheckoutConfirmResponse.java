package com.shop.checkout.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutConfirmResponse {
    private Long orderId;
    private String paymentStatus;
    private LocalDateTime confirmedAt;
    /** 비회원만 채움 */
    private String guestVerificationCode;
}
