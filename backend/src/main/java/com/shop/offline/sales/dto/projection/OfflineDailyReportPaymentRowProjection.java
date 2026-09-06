package com.shop.offline.sales.dto.projection;

public interface OfflineDailyReportPaymentRowProjection {

    Long getOrderInfoId();

    Long getListPrice();

    Long getDiscountAmount();

    Long getTotalAmount();

    Long getItemDiscountAmount();

    String getSourceType();

    Long getPaymentAmount();
}
