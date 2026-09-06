package com.shop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfigDto {
    // 주문 설정 정보
    private String configKey;
    // 주문 설정 값
    private String configValue;
    // 주문 설정 활성화 여부
    private Boolean isEnabled;
}
