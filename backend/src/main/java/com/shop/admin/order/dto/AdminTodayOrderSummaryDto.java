package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTodayOrderSummaryDto {

    private long totalOrderCount;
    private long orderPendingCount;
    private long orderCompletedCount;
    /** {@code ORDER_RECEIPT_COMPLETED} 건수 */
    private long orderReceivedCount;
    /** 취소 제외 {@code totalPaymentAmount} 합(배송비 포함) */
    private long totalOrderAmount;
}
