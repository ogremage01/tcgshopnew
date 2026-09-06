package com.shop.order.dto.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface OrderDailySalesSummaryProjection {

    LocalDate getOrderDate();

    long getTotalOrderCount();

    BigDecimal getTotalOrderAmount();

    BigDecimal getTotalUsedPointAmount();

    BigDecimal getTotalDeliveryFee();

    BigDecimal getTotalActualPaymentAmount();
}
