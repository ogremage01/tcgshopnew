package com.shop.offline.sales.dto.projection;

/**
 * COMPLETED 매출 라인 title별 수량 합 프로젝션.
 */
public interface TitleQuantityProjection {

    String getTitle();

    Long getTotalQuantity();
}
