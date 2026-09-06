package com.shop.offline.sales.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OfflineSalesInfoDto {

    private Long id;
    private String orderId;
    private String orderState;
    private String orderNumber;
    private LocalDateTime createdAt;
    private List<OfflineSalesItemDto> lineItems;
    private List<OfflineSalesPaymentDto> payments;
    private Integer listPrice;
    private Integer discountAmount;
    private Integer taxAmount;
    private Integer supplyAmount;
    private Integer taxExemptAmount;
    private Integer totalAmount;

}
