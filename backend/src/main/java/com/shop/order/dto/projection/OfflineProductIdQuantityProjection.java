package com.shop.order.dto.projection;

/**
 * 비취소 온라인 주문에서 offlineProductId별 수량 합 프로젝션.
 */
public interface OfflineProductIdQuantityProjection {

    String getOfflineProductId();

    Long getTotalQuantity();
}
