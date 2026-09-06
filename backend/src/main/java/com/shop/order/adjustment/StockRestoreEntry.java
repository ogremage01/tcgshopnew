package com.shop.order.adjustment;

import com.shop.order.entity.OrderProduct;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockRestoreEntry {

    private final OrderProduct line;
    private final long quantity;
}
