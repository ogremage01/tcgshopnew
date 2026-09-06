package com.shop.offline.sales.dto.projection;

import java.time.LocalDate;

public interface SalesSummaryProjection {

    LocalDate getPeriodStart();

    Long getTotalOrderCount();

    Long getTotalOrderAmount();

    Long getTotalDiscountAmount();

    Long getTotalTaxAmount();

    Long getTotalSupplyAmount();

    Long getTotalTaxExemptAmount();

    Long getTotalTotalAmount();
}
