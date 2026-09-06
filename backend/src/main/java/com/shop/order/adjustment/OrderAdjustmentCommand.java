package com.shop.order.adjustment;

import com.shop.admin.order.dto.AdminOrderProductModifyRequest;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderAdjustmentCommand {

    private final OrderAdjustmentType type;
    private final Long orderId;
    private final boolean restoreStockOnCancel;
    private final boolean restoreCartOnCancel;
    private final String cancelReason;
    private final AdminOrderProductModifyRequest modifyRequest;

    public static OrderAdjustmentCommand fullCancel(
            Long orderId, boolean restoreStock, boolean restoreCart, String cancelReason) {
        return OrderAdjustmentCommand.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderId(orderId)
                .restoreStockOnCancel(restoreStock)
                .restoreCartOnCancel(restoreCart)
                .cancelReason(cancelReason)
                .build();
    }

    public static OrderAdjustmentCommand partialModify(Long orderId, AdminOrderProductModifyRequest request) {
        return OrderAdjustmentCommand.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderId(orderId)
                .modifyRequest(request)
                .cancelReason(request != null ? request.getCancelReason() : null)
                .build();
    }
}
