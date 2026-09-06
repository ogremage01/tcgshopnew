package com.shop.offline.sales.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesSummaryDto {

    private LocalDate periodStart;
    private Long totalOrderCount;
    private Long totalOrderAmount;
    private Long totalDiscountAmount;
    private Long totalTaxAmount;
    private Long totalSupplyAmount;
    private Long totalTaxExemptAmount;
    private Long totalTotalAmount;
    private List<OfflineSalesSummaryItemDto> items;
    private List<OfflineSalesSummaryPaymentDto> payments;
}
