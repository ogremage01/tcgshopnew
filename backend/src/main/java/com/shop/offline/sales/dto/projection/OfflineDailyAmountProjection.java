package com.shop.offline.sales.dto.projection;

import java.time.LocalDate;

public interface OfflineDailyAmountProjection {

    LocalDate getOrderDate();

    Long getTotalAmount();
}
