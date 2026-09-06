package com.shop.order.dto.projection;

import java.math.BigDecimal;

public interface OrderOnlineDetailProjection {

    /** psm.game — SUPPLY 쿼리에서는 null */
    String getGame();

    /** psm.set_code (카드/밀봉) 또는 psm.supplies_type (서플라이) */
    String getGroupKey();

    /** COUNT(DISTINCT op.product_id) */
    Long getCategoryCount();

    /** SUM(op.quantity) */
    Long getTotalQuantity();

    /** SUM(op.total_price) */
    BigDecimal getTotalAmount();
}
