package com.shop.offline.sales.dto.projection;

import java.time.LocalDate;

public interface SalesSummaryItemProjection {

    LocalDate getPeriodStart();

    String getCategory();

    String getTitle();

    Long getTotalQuantity();

    Long getTotalPriceValue();
}
