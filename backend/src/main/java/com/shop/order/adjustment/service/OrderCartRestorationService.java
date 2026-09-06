package com.shop.order.adjustment.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.cart.dto.CartRestoreLine;
import com.shop.cart.dto.CartRestoreResult;
import com.shop.cart.service.CartService;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.repository.OrderProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCartRestorationService {

    private final CartService cartService;
    private final OrderProductRepository orderProductRepository;

    public CartRestoreResult restoreAllForOrder(OrderInfo orderInfo, Long orderId) {
        if (orderInfo == null || orderId == null) {
            return CartRestoreResult.builder().build();
        }
        if (!Boolean.FALSE.equals(orderInfo.getGuest()) || orderInfo.getUserId() == null) {
            return CartRestoreResult.builder().build();
        }

        List<OrderProduct> orderLines = orderProductRepository.findByOrderInfoId(orderId);
        List<CartRestoreLine> lines = new ArrayList<>();
        int skippedNullSearchMap = 0;

        for (OrderProduct line : orderLines) {
            if (line.getSearchMapId() == null) {
                skippedNullSearchMap++;
                continue;
            }
            long qty = line.getQuantity() != null ? line.getQuantity() : 0L;
            if (qty <= 0) {
                continue;
            }
            lines.add(CartRestoreLine.builder()
                    .searchMapId(line.getSearchMapId())
                    .quantity(qty)
                    .build());
        }

        CartRestoreResult result = cartService.restoreItemsForUserId(orderInfo.getUserId(), lines);
        return CartRestoreResult.builder()
                .linesRequested(result.getLinesRequested())
                .linesFullyRestored(result.getLinesFullyRestored())
                .linesPartiallyRestored(result.getLinesPartiallyRestored())
                .linesSkipped(result.getLinesSkipped() + skippedNullSearchMap)
                .build();
    }
}
