package com.shop.order.dto.projection;

import java.math.BigDecimal;

public interface OrderDailyReportByProductTableProjection {

    String getProductTable();

    Long getCategoryCount();

    Long getQuantity();

    BigDecimal getOrderAmount();
}
