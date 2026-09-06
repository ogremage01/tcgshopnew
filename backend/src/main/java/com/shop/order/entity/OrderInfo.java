package com.shop.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicUpdate
@Table(name = "order_infos", indexes = {
    @Index(name = "idx_order_infos_status_payment_date", columnList = "order_status,payment_date"),
    @Index(name = "idx_order_infos_status_payment_approved_at", columnList = "order_status,payment_approved_at")
})
public class OrderInfo {

    // 주문 정보 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문자 정보:비회원 여부
    @Column(nullable = false)
    private Boolean guest;

    // 주문자 정보:회원 ID
    @Column(nullable = true)
    private Long userId;

    // 수령인 정보:이름
    @Column(nullable = true)
    private String recipientName;

    // 수령인 정보:주소 (AES-256-GCM 암호문)
    @Column(nullable = true, length = 1024)
    private String recipientAddress;

    // 수령인 정보:상세주소 (AES-256-GCM 암호문)
    @Column(nullable = true, length = 1024)
    private String recipientAddressDetail;

    // 수령인 정보:전화번호 (AES-256-GCM 암호문)
    @Column(nullable = true, length = 512)
    private String recipientPhone;

    // 수령인 정보:이메일 (AES-256-GCM 암호문)
    @Column(nullable = true, length = 512)
    private String recipientEmail;

    // 배송 주소 우편번호
    @Column(nullable = true, length = 32)
    private String postalCode;

    // 주문 요청 사항
    @Column(nullable = true)
    private String orderRequest;

    // 주문 상태(주문 취소/주문 대기/주문 완료/접수 완료/배송 완료)
    @Column(nullable = true)
    private String orderStatus;

    // 결제 상태(결제 안됨/결제 됨/결제 취소)
    @Column(nullable = false)
    private String paymentStatus;

    // 배송 회사
    @Column(nullable = true)
    private String deliveryCompany;

    // 배송 송장번호
    @Column(nullable = true)
    private String deliveryTrackingNumber;

    // 배송 메모
    @Column(nullable = true)
    private String deliveryMemo;

    // 결제 통화. KRW/USD
    @Column(nullable = true)
    private String paymentCurrency;

    // 총 상품 금액(배송비를 제외)
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal totalProductAmount;

    // 총 결제 금액
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal totalPaymentAmount;

    // 포인트 사용 금액
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal usedPointAmount;

    // 실질 결제 금액 = 상품금액 + 배송료 - 포인트 사용 금액 (배송료 포함)
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal actualPaymentAmount;

    // 결제 일시
    @Column(nullable = true)
    private LocalDateTime paymentDate;

    // 배송비
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal deliveryFee;

    /** 비회원 주문 확인 보조 코드(6자리 등) */
    @Column(nullable = true, length = 16)
    private String guestVerificationCode;

    /** PG 거래 식별자(연동 시 사용) */
    @Column(nullable = true, length = 128)
    private String pgTransactionId;

    /** 결제 승인 시각 */
    @Column(nullable = true)
    private LocalDateTime paymentApprovedAt;

    /**
     * 주문 상품 전체 수량 합(DB 컬럼 {@code total_quantity}). {@code order_products.quantity} 합계.
     * 목록·요약 표시용으로 주문 확정 시점에 저장한다.
     */
    @Column(name = "total_quantity", nullable = true)
    private Long totalQuantity;

    /**
     * 주문 라인 수(DB 컬럼 {@code order_line_count}). 몇 종류의 상품 행인지,
     * 즉 {@code order_products} 테이블의 해당 주문에 대한 행 개수와 같다.
     */
    @Column(name = "order_line_count", nullable = true)
    private Long orderLineCount;

    /**
     * 결제 방식(DB 컬럼 {@code payment_method}).
     * 카드 / 간편결제 / DIRECT(매장결제) 등.
     */
    @Column(name = "payment_method", nullable = true)
    private String paymentMethod;

    /**
     * 결제 통화 환율 스냅샷
     */
    @Column(name = "payment_currency_rate_snapshot", nullable = true, precision = 19, scale = 4)
    private BigDecimal paymentCurrencyRateSnapshot;

    /**
     * 정산 원화 금액. 환전 수수료는 생각하지 않는다.
     */
    @Column(name = "settle_krw_amount", nullable = true)
    private Long settleKrwAmount;

    /**
     * 주문 확정 시점 적립금 합계 (order_products.reward_points 합산 스냅샷).
     */
    @Column(name = "earned_point_amount", nullable = true)
    private Long earnedPointAmount;

    /**
     * 목록·기간 필터·정렬에 쓰는 기준 시각.
     * {@link #paymentDate}가 있으면 우선, 없으면 {@link #paymentApprovedAt}.
     * 리포지토리 QueryDSL의 {@code CaseBuilder} 조건과 동일한 규칙을 유지할 것.
     */
    public LocalDateTime getEffectiveOrderAt() {
        return paymentDate != null ? paymentDate : paymentApprovedAt;
    }

}
