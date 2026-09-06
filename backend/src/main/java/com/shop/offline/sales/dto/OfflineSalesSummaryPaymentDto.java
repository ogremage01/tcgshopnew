package com.shop.offline.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesSummaryPaymentDto {

    private String sourceType;
    private Long paymentCount;
    private Long totalAmount;
    private Long totalTaxAmount;
    private Long totalSupplyAmount;
    private Long totalTaxExemptAmount;
}
