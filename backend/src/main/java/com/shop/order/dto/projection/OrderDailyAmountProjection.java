package com.shop.order.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일자별 온라인 상품(또는 배송료) 금액 합계 프로젝션.
 */
public interface OrderDailyAmountProjection {

    LocalDate getOrderDate();

    BigDecimal getTotalAmount();
}
