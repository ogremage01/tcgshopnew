package com.shop.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderSimpleDto {
    private Long id;
    private LocalDateTime orderDate;
    private String customerName;
    private String customerContact;
    private String customerEmail;
    private String orderStatus;
    private BigDecimal usedPointAmount;
    private BigDecimal deliveryFee;
    /** 실질 결제 금액 = 상품금액 + 배송료 - 포인트 사용 금액 (배송료 포함). */
    private BigDecimal actualPaymentAmount;
    /** 포인트 차감 전 합계(상품금액 + 배송료). */
    private BigDecimal totalPaymentAmount;
    /** {@code order_products.quantity} 합계(총 개). */
    private Long totalQuantity;
    /** 주문 라인 수(몇 종). */
    private Long orderLineCount;
    // 배송 회사(매장 수령/CJ/우체국/로젠택배 등등....)
    private String deliveryCompany;
    private Boolean guest;
    /** 결제 방식 (카드, DIRECT, TRANSFER 등). */
    private String paymentMethod;

}
