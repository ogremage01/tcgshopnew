package com.shop.offline.sales.dto.projection;

public interface OfflineSalesTotalProjection {

    String getCategory();

    String getItem();

    Long getQuantity();

    Long getAmount();
}
