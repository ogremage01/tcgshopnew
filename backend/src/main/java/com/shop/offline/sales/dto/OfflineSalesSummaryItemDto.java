package com.shop.offline.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesSummaryItemDto {

    private String category;
    private String title;
    private Long totalQuantity;
    private Long totalPriceValue;
}
