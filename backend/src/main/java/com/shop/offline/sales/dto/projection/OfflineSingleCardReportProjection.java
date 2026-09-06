package com.shop.offline.sales.dto.projection;

public interface OfflineSingleCardReportProjection {

    Long getCategoryCount();

    Long getTotalQuantity();

    Long getGrossAmount();

    Long getDiscountAmount();
}
