package com.shop.offline.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OfflineSalesPaymentDto {

    private String sourceType;
    private Integer amount;
    private Integer taxAmount;
    private Integer supplyAmount;
    private Integer taxExemptAmount;
}
