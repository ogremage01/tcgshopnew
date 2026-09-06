package com.shop.order.dto.projection;

import java.math.BigDecimal;

public interface OrderDailyReportByPaymentMethodProjection {

    String getPaymentGroup();

    Long getOrderCount();

    BigDecimal getOrderAmount();

    BigDecimal getUsedPointAmount();

    BigDecimal getActualPaymentAmount();
}
