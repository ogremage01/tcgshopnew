package com.shop.admin.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderDetailDto {
    private AdminOrderInfoDto orderInfo;
    /** 카드 주문 라인 — 게임별 그룹(표시 순서는 서버에서 결정). */
    private List<AdminOrderCardProductGroupDto> orderCardProductGroups;
    private List<AdminOrderManualProductDto> orderManualProducts;
    private List<AdminOrderSealedProductDto> orderSealedProducts;
    private List<AdminOrderSupplyProductDto> orderSupplyProducts;
}
