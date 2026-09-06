package com.shop.order.adjustment.stock;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.shop.search.dto.enums.ProductTableEnum;

@Component
public class OrderStockReleaseHandlerRegistry {

    private final EnumMap<ProductTableEnum, OrderStockReleaseHandler> handlers =
            new EnumMap<>(ProductTableEnum.class);

    public OrderStockReleaseHandlerRegistry(List<OrderStockReleaseHandler> handlerList) {
        for (OrderStockReleaseHandler handler : handlerList) {
            if (handlers.put(handler.table(), handler) != null) {
                throw new IllegalStateException("Duplicate OrderStockReleaseHandler for " + handler.table());
            }
        }
    }

    public Optional<OrderStockReleaseHandler> find(ProductTableEnum table) {
        return Optional.ofNullable(handlers.get(table));
    }

    public OrderStockReleaseHandler require(ProductTableEnum table) {
        OrderStockReleaseHandler handler = handlers.get(table);
        if (handler == null) {
            throw new IllegalStateException("No OrderStockReleaseHandler for " + table);
        }
        return handler;
    }
}
