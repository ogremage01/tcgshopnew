package com.shop.offline.sales.dto.projection;

public interface OfflineDailyReportByLinkTableProjection {

    String getLinkTableName();

    Long getCategoryCount();

    Long getTotalQuantity();

    Long getGrossAmount();

    Long getDiscountAmount();
}
