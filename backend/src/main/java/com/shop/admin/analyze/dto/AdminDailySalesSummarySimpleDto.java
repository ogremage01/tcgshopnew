package com.shop.admin.analyze.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDailySalesSummarySimpleDto {

    private LocalDate orderDate;
    //주문 건수(취소 제외)
    private long totalOrderCount;
    //주문 금액(취소 제외)
    private long totalOrderAmount;
    //주문 포인트 사용 금액(취소 제외)
    private long totalUsedPointAmount;
    //주문 배송료 금액(취소 제외)
    private long totalDeliveryFee;
    //주문 실결제 금액(취소 제외)
    private long totalActualPaymentAmount;
}
