package com.shop.scheduler.price.service.link;

import java.util.List;

public interface PriceLinkService {

    // 가격 링크 서비스

    /**
     * mtg_prices 중 tcgPPriceId가 없는 행에 매칭되는 tcg_p_prices.id와 conNumName을 연결한다.
     */
    void syncMtgPriceWithTcgPrice();

    /**
     * fab_prices 중 tcgPPriceId가 없는 행에 매칭되는 tcg_p_prices.id와 conNumName을 연결한다.
     */
    void syncFabPriceWithTcgPrice();

    /**
     * 이미 tcg_p_price_id만 연결된 레거시 행에 대해 tcg_p_prices.ob_price_id를 채운다.
     */
    void backfillTcgObPriceIdFromMtgLinks();

    /**
     * 이미 tcg_p_price_id만 연결된 레거시 행에 대해 tcg_p_prices.ob_price_id를 채운다.
     */
    void backfillTcgObPriceIdFromFabLinks();

    boolean startSyncPriceLink();

    /**
     * PK 커서로 놓칠 수 있는 NULL 행을 다시 스캔해 링크한다(갭 정비). 별도 sync_log 대상.
     */
    boolean startSyncPriceLinkRescanNulls();

    /**
     * 기본 링크와 동일 조인이지만 m.id/f.id 커서 없이 NULL 행만 반복 처리한다.
     */
    void syncMtgPriceWithTcgPriceRescanNulls();

    void syncFabPriceWithTcgPriceRescanNulls();

    boolean isSyncPriceLinkRunning();

    /**
     * SELECT로 (targetId, sourceId) 쌍을 커서 기반으로 읽어
     * UPDATE targetTable SET col = sourceId WHERE id = targetId 형태로 PK 갱신한다.
     *
     * selectSql 요구사항:
     * - col1: targetId, col2: sourceId
     * - WHERE 절에 "AND targetId > ?" 커서 파라미터 포함 (파라미터 1)
     * - ORDER BY targetId ASC
     * - LIMIT ? (파라미터 2)
     *
     * @param selectSql 커서 기반 SELECT SQL
     * @param updateSql UPDATE SQL
     * @return 갱신된 총 행 수
     */
    int runSelectThenUpdate(String selectSql, String updateSql);

    <T> List<List<T>> partition(List<T> list, int size);

    /**
     * check_code/check_code_refined만 NULL로 초기화 후 재계산한다. ID는 건드리지 않는다.
     */
    boolean startRebuildCheckCodes();

    /**
     * NULL 초기화 없이 재계산한다. 행마다 check_code와 check_code_refined가 같으면 둘 다 갱신하고,
     * 다르면(DUPLICATE_CODE 등으로 refined가 분리된 경우) check_code만 갱신한다.
     */
    boolean startRebuildCheckCodesSelective();

    /**
     * tcgPPriceId/obPriceId만 NULL로 초기화 후 재연결한다. check_code는 건드리지 않는다.
     */
    boolean startRebuildLinks();

    /**
     * check_code 재빌드(op1) → 링크 재연결(op2)을 단일 플래그로 순차 실행한다.
     */
    boolean startFullRebuild();
}
