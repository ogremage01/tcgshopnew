package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderDeliveryInfoUpdateRequest {
    private String deliveryTrackingNumber;
    private String deliveryMemo;
}
