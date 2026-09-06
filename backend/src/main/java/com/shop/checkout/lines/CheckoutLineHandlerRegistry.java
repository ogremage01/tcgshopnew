package com.shop.checkout.lines;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.shop.search.dto.enums.ProductTableEnum;

@Component
public class CheckoutLineHandlerRegistry {

    private final EnumMap<ProductTableEnum, CheckoutLineHandler> handlers = new EnumMap<>(ProductTableEnum.class);

    public CheckoutLineHandlerRegistry(List<CheckoutLineHandler> handlerList) {
        for (CheckoutLineHandler h : handlerList) {
            if (handlers.put(h.table(), h) != null) {
                throw new IllegalStateException("Duplicate CheckoutLineHandler for " + h.table());
            }
        }
    }

    public Optional<CheckoutLineHandler> find(ProductTableEnum table) {
        return Optional.ofNullable(handlers.get(table));
    }

    public CheckoutLineHandler require(ProductTableEnum table) {
        CheckoutLineHandler h = handlers.get(table);
        if (h == null) {
            throw new IllegalStateException("No CheckoutLineHandler for " + table);
        }
        return h;
    }
}
