package com.shop.order.enums;

import java.util.Optional;

public enum OrderStatus {

    // 주문 상태들
    /** 주문 완료 */
    ORDER_COMPLETED("ORDER_COMPLETED"),
    /** 주문 취소 */
    ORDER_CANCELLED("ORDER_CANCELLED"),
    /** 결제 확인중 */
    ORDER_PENDING("ORDER_PENDING"),
    /** 접수 완료 */
    ORDER_RECEIPT_COMPLETED("ORDER_RECEIPT_COMPLETED"),
    /** 발송 완료 */
    ORDER_DELIVERY_COMPLETED("ORDER_DELIVERY_COMPLETED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<OrderStatus> fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (OrderStatus status : values()) {
            if (status.value.equals(raw)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}
