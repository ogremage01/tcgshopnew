package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTotalOrderSummaryDto {

    private long orderPendingCount;
    private long orderCompletedCount;
    private long orderReceivedCount;
}
