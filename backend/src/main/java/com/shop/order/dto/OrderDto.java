package com.shop.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    // 같은 패키지이므로 import 없이 사용 가능
    // 주문 정보
    private OrderInfoDto orderInfo;
    // 같은 패키지이므로 import 없이 사용 가능
    // 주문 상품 목록
    private List<OrderProductDto> orderProducts;

}
