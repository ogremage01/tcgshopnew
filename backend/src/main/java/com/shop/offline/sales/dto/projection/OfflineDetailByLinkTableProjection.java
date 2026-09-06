package com.shop.offline.sales.dto.projection;

/**
 * 오프라인 판매 항목을 링크 테이블(상품 분류) + 세부 카테고리 단위로 집계한 프로젝션.
 *
 * <p>linkTableName(Sealed/Supply/Manual)별로 1차 분류하고, 연결된 온라인 상품의
 * 세부 카테고리(set_code / supplies_type / manual_category)를 groupKey로 반환한다.
 * 간편등록 등으로 온라인 상품과 연결되지 않은 항목은 groupKey가 null 로 내려온다.</p>
 */
public interface OfflineDetailByLinkTableProjection {

    /** 링크 테이블 이름: Sealed / Supply / Manual */
    String getLinkTableName();

    /** 연결된 온라인 상품의 게임 이름(product_search_maps.game) — 미분류 시 null */
    String getGame();

    /** 세부 카테고리(set_code / supplies_type / manual_category) — 미분류 시 null */
    String getGroupKey();

    /** COUNT(DISTINCT i.title) */
    Long getCategoryCount();

    /** SUM(i.quantity) */
    Long getTotalQuantity();

    /** SUM(i.price_value * i.quantity) */
    Long getTotalAmount();
}
