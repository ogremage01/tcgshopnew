package com.shop.offline.sales.dto;

public enum OfflineSalesSummaryPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTER,
    YEARLY,
    TOTAL;

    public static OfflineSalesSummaryPeriod from(String value) {
        return OfflineSalesSummaryPeriod.valueOf(value.toUpperCase());
    }
}
