package com.shop.checkout.dto;

import java.math.BigDecimal;

import com.shop.checkout.domain.DeliveryMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchCheckoutDraftRequest {
    private DeliveryMethod deliveryMethod;
    private String recipientName;
    private String recipientAddress;
    private String recipientAddressDetail;
    private String recipientPostalCode;
    private String recipientPhone;
    private String recipientEmail;
    private String orderRequest;
    /** 사용 포인트 (원 단위, 서버에서 잔액 재검증) */
    private Long usedPointAmount;
    /** 결제 통화. 현재 KRW 고정(USD 흐름 배제). 요청값이 있어도 서버가 KRW로 덮어쓴다. */
    private String paymentCurrency;
    /** 결제 통화 환율. 현재 미사용(KRW 1:1). DB 컬럼 유지용 */
    private BigDecimal paymentCurrencyRate;
    /** 정산 원화 금액 */
    private Long settleKrwAmount;

}
