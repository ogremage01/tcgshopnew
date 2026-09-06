package com.shop.checkout.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.shop.checkout.domain.DeliveryMethod;
import com.shop.checkout.domain.DraftStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutDraftResponse {
    private String publicId;
    private DraftStatus status;
    private DeliveryMethod deliveryMethod;
    private String recipientName;
    private String recipientAddress;
    private String recipientAddressDetail;
    private String recipientPostalCode;
    private String recipientPhone;
    private String recipientEmail;
    private String orderRequest;
    private BigDecimal subtotalAmount;
    private BigDecimal deliveryFee;
    /** 무료배송 적용 전 기준 배송비(택배 배송 시). 실제 청구액은 {@link #deliveryFee}. */
    private BigDecimal standardDeliveryFee;
    /** 무료배송 기준 금액. 비활성이면 null. */
    private BigDecimal freeShippingThreshold;
    private BigDecimal usedPointAmount;
    private BigDecimal totalAmount;
    private BigDecimal totalProductAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Long confirmedOrderId;
    private List<CheckoutDraftItemResponse> items;
    private Boolean hasEventTicket;
}
