package com.shop.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSimpleDto {

    // 목록 표시를 위한 주문 간략 정보 Dto

    private Long id;
    private LocalDateTime orderDate;
    private String orderStatus;
    private BigDecimal orderTotal;
    private String paymentCurrency;
}
