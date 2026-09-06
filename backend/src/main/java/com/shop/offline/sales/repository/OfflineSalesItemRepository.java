package com.shop.offline.sales.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.offline.sales.dto.projection.OfflineDailyAmountProjection;
import com.shop.offline.sales.dto.projection.OfflineDailyReportByLinkTableProjection;
import com.shop.offline.sales.dto.projection.OfflineDetailByLinkTableProjection;
import com.shop.offline.sales.dto.projection.OfflineSalesTotalProjection;
import com.shop.offline.sales.dto.projection.OfflineSingleCardReportProjection;
import com.shop.offline.sales.dto.projection.SalesSummaryItemProjection;
import com.shop.offline.sales.dto.projection.TitleQuantityProjection;
import com.shop.offline.sales.entity.OfflineSalesItem;

public interface OfflineSalesItemRepository extends JpaRepository<OfflineSalesItem, Long> {

    Optional<OfflineSalesItem> findFirstByLineItemIdOrderByIdAsc(String lineItemId);

    List<OfflineSalesItem> findAllByLineItemId(String lineItemId);

    List<OfflineSalesItem> findAllByOfflineSalesInfoId(Long offlineSalesInfoId);

    /**
     * 일별 판매 요약 데이터 조회
     * @param periodStarts 기간 시작일
     * @return 판매 요약 데이터
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS periodStart,
                   i.category AS category,
                   i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalPriceValue
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND DATE(o.created_at) IN (:periodStarts)
            GROUP BY DATE(o.created_at), i.category, i.title
            ORDER BY DATE(o.created_at) DESC, i.category, i.title
            """,
            nativeQuery = true)
    List<SalesSummaryItemProjection> dailyItemSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 주별 판매 요약 데이터 조회
     * @param periodStarts 기간 시작일
     * @return 판매 요약 데이터
     */
    @Query(value = """
            SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS periodStart,
                   i.category AS category,
                   i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalPriceValue
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) IN (:periodStarts)
            GROUP BY DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY),
                     i.category, i.title
            ORDER BY periodStart DESC, i.category, i.title
            """,
            nativeQuery = true)
    List<SalesSummaryItemProjection> weeklyItemSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 월별 판매 요약 데이터 조회
     * @param periodStarts 기간 시작일
     * @return 판매 요약 데이터
     */
    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   i.category AS category,
                   i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalPriceValue
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(
                      CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                      '%Y-%m-%d'
                  ) IN (:periodStarts)
            GROUP BY YEAR(o.created_at), MONTH(o.created_at), i.category, i.title
            ORDER BY YEAR(o.created_at) DESC, MONTH(o.created_at) DESC, i.category, i.title
            """,
            nativeQuery = true)
    List<SalesSummaryItemProjection> monthlyItemSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 분기별 판매 요약 데이터 조회
     * @param periodStarts 기간 시작일
     * @return 판매 요약 데이터
     */
    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(
                           YEAR(o.created_at), '-',
                           LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                       ),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   i.category AS category,
                   i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalPriceValue
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(
                      CONCAT(
                          YEAR(o.created_at), '-',
                          LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                      ),
                      '%Y-%m-%d'
                  ) IN (:periodStarts)
            GROUP BY YEAR(o.created_at), QUARTER(o.created_at), i.category, i.title
            ORDER BY YEAR(o.created_at) DESC, QUARTER(o.created_at) DESC, i.category, i.title
            """,
            nativeQuery = true)
    List<SalesSummaryItemProjection> quarterlyItemSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 연별 판매 요약 데이터 조회
     * @param periodStarts 기간 시작일
     * @return 판매 요약 데이터
     */
    @Query(value = """
            SELECT STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') AS periodStart,
                   i.category AS category,
                   i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalPriceValue
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') IN (:periodStarts)
            GROUP BY YEAR(o.created_at), i.category, i.title
            ORDER BY YEAR(o.created_at) DESC, i.category, i.title
            """,
            nativeQuery = true)
    List<SalesSummaryItemProjection> yearlyItemSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 전체 판매 요약 데이터 조회
     * @return 판매 요약 데이터
     */
    @Query(value = """
            SELECT NULL AS periodStart,
                   i.category AS category,
                   i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalPriceValue
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
            GROUP BY i.category, i.title
            ORDER BY i.category, i.title
            """,
            nativeQuery = true)
    List<SalesSummaryItemProjection> totalItemSummary();

    /**
     * 특정 날짜의 오프라인 판매 항목을 링크 테이블(상품 분류)별로 집계한다.
     *
     * <p>링크 테이블은 offline_products.link_table_name에서 가져오며,
     * 상품 마스터에 없는 항목(링크 없음)은 'Manual'로 처리한다.</p>
     *
     * <p>할인 금액(discountAmount): offline_sales_item_discounts 테이블에서 아이템 단위로 합산한다.</p>
     *
     * <p>excludedTitle: "Single cards" 같이 별도 섹션에서 처리되는 상품을 집계에서 제외한다.</p>
     *
     * <p>grossAmount = price_value × quantity (할인 전 판매가 × 수량)</p>
     */
    @Query(value = """
            SELECT COALESCE(p.link_table_name, 'Manual') AS linkTableName,
                   COUNT(DISTINCT i.title) AS categoryCount,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS grossAmount,
                   COALESCE(SUM(item_discount.discount_amount), 0) AS discountAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT title, MIN(link_table_name) AS link_table_name
                FROM offline_products
                GROUP BY title
            ) p ON p.title = i.title
            LEFT JOIN (
                SELECT offline_sales_item_id, COALESCE(SUM(amount), 0) AS discount_amount
                FROM offline_sales_item_discounts
                GROUP BY offline_sales_item_id
            ) item_discount ON item_discount.offline_sales_item_id = i.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :day
              AND o.created_at < :day + INTERVAL 1 DAY
              AND (i.title IS NULL OR i.title <> :excludedTitle)
            GROUP BY COALESCE(p.link_table_name, 'Manual')
            ORDER BY linkTableName
            """,
            nativeQuery = true)
    List<OfflineDailyReportByLinkTableProjection> findDailyReportByLinkTableName(
            @Param("day") LocalDate day,
            @Param("orderState") String orderState,
            @Param("excludedTitle") String excludedTitle);

    /**
     * 특정 날짜의 오프라인 아이템 할인 금액 전체 합계를 조회한다.
     *
     * <p>레포트의 "총 할인 금액" 행에 표시되며, offline_sales_item_discounts를 기준으로 합산한다.
     * excludedTitle에 해당하는 상품(예: "Single cards")은 제외한다.</p>
     */
    @Query(value = """
            SELECT COALESCE(SUM(item_discount.discount_amount), 0) AS discountAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT offline_sales_item_id, COALESCE(SUM(amount), 0) AS discount_amount
                FROM offline_sales_item_discounts
                GROUP BY offline_sales_item_id
            ) item_discount ON item_discount.offline_sales_item_id = i.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :day
              AND o.created_at < :day + INTERVAL 1 DAY
              AND (i.title IS NULL OR i.title <> :excludedTitle)
            """,
            nativeQuery = true)
    Long findDailyReportTotalItemDiscountAmount(
            @Param("day") LocalDate day,
            @Param("orderState") String orderState,
            @Param("excludedTitle") String excludedTitle);

    /**
     * 지정 기간의 일자별 오프라인 상품 판매 금액 합계를 조회한다.
     *
     * <p>일별 판매 레포트 목록 화면에서 오프라인 컬럼값을 채우기 위해 사용한다.
     * grossAmount = price_value × quantity 이며, excludedTitle 상품은 제외된다.</p>
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS orderDate,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
              AND (i.title IS NULL OR i.title <> :excludedTitle)
            GROUP BY DATE(o.created_at)
            ORDER BY DATE(o.created_at) DESC
            """,
            nativeQuery = true)
    List<OfflineDailyAmountProjection> findDailyItemAmountByPeriodExcludingTitle(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState,
            @Param("excludedTitle") String excludedTitle);

    /**
     * 지정 기간의 일자별 오프라인 상품 판매 금액 합계를 조회한다. (싱글카드 포함)
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS orderDate,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
            GROUP BY DATE(o.created_at)
            ORDER BY DATE(o.created_at) DESC
            """,
            nativeQuery = true)
    List<OfflineDailyAmountProjection> findDailyItemAmountByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState);

    /**
     * 지정 기간(시작~종료) 범위의 오프라인 아이템 링크 테이블별 집계를 조회한다. (주간·월간 보고서용)
     */
    @Query(value = """
            SELECT COALESCE(p.link_table_name, 'Manual') AS linkTableName,
                   COUNT(DISTINCT i.title) AS categoryCount,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS grossAmount,
                   COALESCE(SUM(item_discount.discount_amount), 0) AS discountAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT title, MIN(link_table_name) AS link_table_name
                FROM offline_products
                GROUP BY title
            ) p ON p.title = i.title
            LEFT JOIN (
                SELECT offline_sales_item_id, COALESCE(SUM(amount), 0) AS discount_amount
                FROM offline_sales_item_discounts
                GROUP BY offline_sales_item_id
            ) item_discount ON item_discount.offline_sales_item_id = i.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
              AND (i.title IS NULL OR i.title <> :excludedTitle)
            GROUP BY COALESCE(p.link_table_name, 'Manual')
            ORDER BY linkTableName
            """,
            nativeQuery = true)
    List<OfflineDailyReportByLinkTableProjection> findReportByLinkTableNameByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState,
            @Param("excludedTitle") String excludedTitle);

    /**
     * 지정 기간(시작~종료) 범위의 오프라인 판매 항목을 링크 테이블(Sealed/Supply/Manual) +
     * 연결된 온라인 상품의 세부 카테고리 단위로 집계한다. (오프라인 매출 세부 섹션용)
     *
     * <p>offline_products.title로 연결 정보를 찾고, link_id를 product_search_maps.source_id와
     * 매칭해 게임(game) 및 세부 카테고리(set_code / supplies_type / manual_category)를 가져온다.
     * 밀봉(Sealed) 제품은 온라인과 동일하게 게임 단위까지 분할하기 위해 game도 GROUP BY 한다.
     * 간편등록 등으로 연결이 없는 항목은 링크 테이블이 'Manual'로, 게임/세부 카테고리(groupKey)는
     * null 로 내려온다(집계 단계에서 "분류 없음"으로 처리).</p>
     *
     * <p>한 title이 offline_products에 여러 행으로 존재할 수 있으므로, 대표 행을
     * link_table_name = MIN(link_table_name)으로 먼저 고정한 뒤 그 분류에 속한 행들에서만
     * link_id를 취한다. link_table_name과 link_id를 각각 독립적으로 MIN 하면 서로 다른
     * 행의 값이 섞여 잘못된 (분류, link_id) 조합이 생길 수 있어 이를 방지한다.</p>
     *
     * <p>excludedTitle(예: "Single cards")은 별도 섹션에서 처리하므로 제외한다.</p>
     */
    @Query(value = """
            SELECT COALESCE(p.link_table_name, 'Manual') AS linkTableName,
                   psm.game AS game,
                   CASE COALESCE(p.link_table_name, 'Manual')
                       WHEN 'Sealed' THEN psm.set_code
                       WHEN 'Supply' THEN psm.supplies_type
                       ELSE psm.manual_category
                   END AS groupKey,
                   COUNT(DISTINCT i.title) AS categoryCount,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS totalAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT lt.title,
                       lt.link_table_name,
                       MIN(op.link_id) AS link_id
                FROM (
                    SELECT title, MIN(link_table_name) AS link_table_name
                    FROM offline_products
                    GROUP BY title
                ) lt
                INNER JOIN offline_products op
                    ON op.title = lt.title
                    AND op.link_table_name = lt.link_table_name
                GROUP BY lt.title, lt.link_table_name
            ) p ON p.title = i.title
            LEFT JOIN product_search_maps psm ON psm.source_id = p.link_id
                AND psm.table_name = CASE COALESCE(p.link_table_name, 'Manual')
                    WHEN 'Sealed' THEN 'SEALED_PRODUCT'
                    WHEN 'Supply' THEN 'SUPPLY'
                    ELSE 'MANUAL_PRODUCT'
                END
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
              AND (i.title IS NULL OR i.title <> :excludedTitle)
            GROUP BY linkTableName, game, groupKey
            ORDER BY linkTableName, game, totalAmount DESC
            """,
            nativeQuery = true)
    List<OfflineDetailByLinkTableProjection> findOfflineDetailByLinkTableByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState,
            @Param("excludedTitle") String excludedTitle);

    /**
     * 특정 날짜의 싱글카드(title = singleCardsTitle) 아이템 집계를 조회한다. (일별 보고서용)
     */
    @Query(value = """
            SELECT COUNT(DISTINCT i.title) AS categoryCount,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS grossAmount,
                   COALESCE(SUM(item_discount.discount_amount), 0) AS discountAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT offline_sales_item_id, COALESCE(SUM(amount), 0) AS discount_amount
                FROM offline_sales_item_discounts
                GROUP BY offline_sales_item_id
            ) item_discount ON item_discount.offline_sales_item_id = i.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :day
              AND o.created_at < :day + INTERVAL 1 DAY
              AND i.title = :singleCardsTitle
            """,
            nativeQuery = true)
    OfflineSingleCardReportProjection findDailySingleCardReport(
            @Param("day") LocalDate day,
            @Param("orderState") String orderState,
            @Param("singleCardsTitle") String singleCardsTitle);

    /**
     * 지정 기간(시작~종료) 범위의 싱글카드(title = singleCardsTitle) 아이템 집계를 조회한다. (주간·월간 보고서용)
     */
    @Query(value = """
            SELECT COUNT(DISTINCT i.title) AS categoryCount,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS grossAmount,
                   COALESCE(SUM(item_discount.discount_amount), 0) AS discountAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT offline_sales_item_id, COALESCE(SUM(amount), 0) AS discount_amount
                FROM offline_sales_item_discounts
                GROUP BY offline_sales_item_id
            ) item_discount ON item_discount.offline_sales_item_id = i.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
              AND i.title = :singleCardsTitle
            """,
            nativeQuery = true)
    OfflineSingleCardReportProjection findSingleCardReportByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState,
            @Param("singleCardsTitle") String singleCardsTitle);

    /**
     * 지정 기간(시작~종료) 범위의 오프라인 아이템 할인 금액 전체 합계를 조회한다. (주간·월간 보고서용)
     */
    @Query(value = """
            SELECT COALESCE(SUM(item_discount.discount_amount), 0) AS discountAmount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT offline_sales_item_id, COALESCE(SUM(amount), 0) AS discount_amount
                FROM offline_sales_item_discounts
                GROUP BY offline_sales_item_id
            ) item_discount ON item_discount.offline_sales_item_id = i.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
              AND (i.title IS NULL OR i.title <> :excludedTitle)
            """,
            nativeQuery = true)
    Long findReportTotalItemDiscountAmountByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState,
            @Param("excludedTitle") String excludedTitle);


            /**
             * 지정 기간(시작~종료) 범위의 오프라인 아이템 매출 금액 전체 합계를 조회한다.
             */

    @Query(value = """
            SELECT i.category AS category,
                   i.title AS item,
                   COALESCE(SUM(i.quantity), 0) AS quantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS amount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON o.id = i.offline_sales_info_id
            WHERE o.order_state = 'COMPLETED'
              AND o.created_at >= :startDate
              AND o.created_at <= :endDate
            GROUP BY i.category, i.title
            ORDER BY amount DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM offline_sales_items i
                INNER JOIN offline_sales_infos o ON o.id = i.offline_sales_info_id
                WHERE o.order_state = 'COMPLETED'
                  AND o.created_at >= :startDate
                  AND o.created_at <= :endDate
                GROUP BY i.category, i.title
            ) grouped
            """,
            nativeQuery = true)
    Page<OfflineSalesTotalProjection> findOfflineSalesTotal(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * 지정 기간(시작~종료) 범위의 오프라인 아이템 매출 금액 전체 합계를 조회한다.
     */
    @Query(value = """
            SELECT i.category AS category,
                   i.title AS item,
                   COALESCE(SUM(i.quantity), 0) AS quantity,
                   COALESCE(SUM(i.price_value * i.quantity), 0) AS amount
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON o.id = i.offline_sales_info_id
            WHERE o.order_state = 'COMPLETED'
              AND o.created_at >= :startDate
              AND o.created_at <= :endDate
            GROUP BY i.category, i.title
            ORDER BY amount DESC
            """,
            nativeQuery = true)
    List<OfflineSalesTotalProjection> findOfflineSalesTotalList(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * COMPLETED 주문 라인의 title별 수량 합 (출고 전량 재집계용).
     */
    @Query(value = """
            SELECT i.title AS title,
                   COALESCE(SUM(i.quantity), 0) AS totalQuantity
            FROM offline_sales_items i
            INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND i.title IS NOT NULL
              AND i.title <> ''
            GROUP BY i.title
            """,
            nativeQuery = true)
    List<TitleQuantityProjection> sumCompletedQuantityGroupedByTitle();
}
