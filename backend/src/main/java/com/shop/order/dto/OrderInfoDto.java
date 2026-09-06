package com.shop.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrderInfoDto {
    // 주문 정보 Dto

    // 주문 정보
    private Long id;
    // 주문자 정보:비회원 여부
    private Boolean guest;
    // 주문자 정보:회원 ID
    private Long userId;
    // 수령인 정보:이름
    private String recipientName;
    // 수령인 정보:주소
    private String recipientAddress;
    // 수령인 정보:상세주소
    private String recipientAddressDetail;
    // 수령인 정보:전화번호
    private String recipientPhone;
    // 수령인 정보:이메일
    private String recipientEmail;
    // 주문 요청 사항
    private String orderRequest;
    // 주문 상태(주문 취소/주문 대기/주문 완료/접수 완료/배송 완료)
    private String orderStatus;
    // 결제 상태(결제 안됨/결제 됨/결제 취소)
    private String paymentStatus;
    // 배송 회사
    private String deliveryCompany;
    // 배송 송장번호
    private String deliveryTrackingNumber;
    // 배송 메모
    private String deliveryMemo;
    // 결제 통화
    private String paymentCurrency;
    // 총 상품 금액(배송비 제외)
    private BigDecimal totalProductAmount;
    // 총 결제 금액
    private BigDecimal totalPaymentAmount;
    // 포인트 사용 금액
    private BigDecimal usedPointAmount;
    // 실질 결제 금액(총 금액 - 포인트 사용 금액)
    private BigDecimal actualPaymentAmount;
    // 결제 일시
    private LocalDateTime paymentDate;
    // 배송비
    private BigDecimal deliveryFee;
    // 주문 상품 수량(총 개)
    private Long totalQuantity;
    // 주문 라인 수(몇 종)
    private Long orderLineCount;
    // 결제 방식(카드/매장결제/무통장?)
    private String paymentMethod;
    /** PG 거래 식별자 (토스 paymentKey 등) */
    private String pgTransactionId;
    // 결제 통화 환율 스냅샷
    private BigDecimal paymentCurrencyRateSnapshot;
    // 주문 내 총 적립금 합계
    private Long totalEarnedPoints;
    // 배송 주소 우편번호
    private String postalCode;
}
