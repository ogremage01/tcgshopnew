package com.shop.offline.sales.dto.projection;

import java.time.LocalDate;

public interface PaymentSummaryProjection {

    LocalDate getPeriodStart();

    String getSourceType();

    Long getPaymentCount();

    Long getTotalAmount();

    Long getTotalTaxAmount();

    Long getTotalSupplyAmount();

    Long getTotalTaxExemptAmount();
}
