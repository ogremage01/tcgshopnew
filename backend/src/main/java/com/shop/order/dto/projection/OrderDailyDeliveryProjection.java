package com.shop.order.dto.projection;

import java.math.BigDecimal;

public interface OrderDailyDeliveryProjection {

    /** 배송료가 0보다 큰 주문 건수 */
    Long getDeliveryCount();

    /** 배송료 합계 */
    BigDecimal getDeliveryFeeSum();
}
