package com.shop.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDailySalesSummaryDto {

    private LocalDate orderDate;
    private long totalOrderCount;
    private BigDecimal totalOrderAmount;
    private BigDecimal totalUsedPointAmount;
    private BigDecimal totalDeliveryFee;
    private BigDecimal totalActualPaymentAmount;
}
