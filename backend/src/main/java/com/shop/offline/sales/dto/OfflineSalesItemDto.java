package com.shop.offline.sales.dto;

import java.util.List;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OfflineSalesItemDto {

    private Long id;
    private String orderId;
    private String title;
    private String category;
    private Integer priceUnit;
    private Integer priceValue;
    private Integer quantity;
    private String memo;
    private List<OfflineSalesAppliedDiscountDto> appliedDiscounts;

}
