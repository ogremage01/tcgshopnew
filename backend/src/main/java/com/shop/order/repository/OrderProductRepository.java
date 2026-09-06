package com.shop.order.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.order.dto.projection.OfflineProductIdQuantityProjection;
import com.shop.order.dto.projection.OrderDailyAmountProjection;
import com.shop.order.dto.projection.OrderDailyReportByPaymentMethodProjection;
import com.shop.order.dto.projection.OrderDailyReportByProductTableProjection;
import com.shop.order.dto.projection.OrderDailyReportTotalAmountProjection;
import com.shop.order.dto.projection.OrderOnlineDetailProjection;
import com.shop.order.entity.OrderProduct;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    List<OrderProduct> findByOrderInfoId(Long orderInfoId);
    List<OrderProduct> findByOrderInfoIdAndProductType(Long orderInfoId, String productType);

    /**
     * 결제 수단 그룹별 상품 합계 금액을 조회한다. (order_products 기준)
     *
     * <p>order_infos에는 total_product_amount가 있지만, 상품 테이블별로 쪼개어 집계하려면
     * order_products의 total_price를 직접 합산해야 한다.
     * 이 쿼리는 그 상품 합계 금액(orderAmount)만 제공하며,
     * usedPointAmount·actualPaymentAmount는 항상 0으로 반환된다.
     * → {@link OrderInfoRepository#findDailyReportByPaymentMethod}와 병합해서 사용한다.</p>
     */
    @Query(value = """
            SELECT
                CASE
                    WHEN o.payment_method = 'DIRECT' THEN 'DIRECT'
                    WHEN o.payment_method = '카드' THEN 'ONLINE_CARD'
                    WHEN o.payment_method = '간편결제' THEN 'ONLINE_EASY_PAY'
                    ELSE 'ONLINE_OTHER'
                END AS paymentGroup,
                COUNT(DISTINCT o.id) AS orderCount,
                COALESCE(SUM(op.total_price), 0) AS orderAmount,
                0 AS usedPointAmount,
                0 AS actualPaymentAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            WHERE o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY CASE
                WHEN o.payment_method = 'DIRECT' THEN 'DIRECT'
                WHEN o.payment_method = '카드' THEN 'ONLINE_CARD'
                WHEN o.payment_method = '간편결제' THEN 'ONLINE_EASY_PAY'
                ELSE 'ONLINE_OTHER'
            END
            """, nativeQuery = true)
    List<OrderDailyReportByPaymentMethodProjection> findDailyReportLineAmountByPaymentMethod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 상품 테이블(product_table 컬럼) 기준으로 주문 수량·금액을 집계한다.
     *
     * <p>product_table이 NULL인 행은 'UNION_PRICE'로 처리한다.
     * categoryCount는 동일 product_id를 중복 제거한 고유 상품 종류 수이다.</p>
     *
     * <p>반환값은 {@link com.shop.admin.analyze.service.OnlineSalesAggregator#aggregateProductTable}에서
     * CARD_PRODUCT / SEALED_PRODUCT / SUPPLY / 기타로 분류된다.</p>
     */
    @Query(value = """
            SELECT
                COALESCE(op.product_table, 'UNION_PRICE') AS productTable,
                COUNT(DISTINCT op.product_id) AS categoryCount,
                COALESCE(SUM(op.quantity), 0) AS quantity,
                COALESCE(SUM(op.total_price), 0) AS orderAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            WHERE o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY COALESCE(op.product_table, 'UNION_PRICE')
            """, nativeQuery = true)
    List<OrderDailyReportByProductTableProjection> findDailyReportByProductTable(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 지정 기간의 온라인 주문 상품 금액 전체 합계를 조회한다.
     *
     * <p>레포트의 "온라인 합계" 행에 표시할 단일 숫자로,
     * order_products.total_price를 모두 더한 값이다.</p>
     */
    @Query(value = """
            SELECT COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            WHERE o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            """, nativeQuery = true)
    OrderDailyReportTotalAmountProjection findDailyReportTotalAmount(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 지정 기간의 일자별 온라인 상품 금액 합계를 조회한다. ({@code order_products.total_price}, DIRECT 포함)
     */
    @Query(value = """
            SELECT
                DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END) AS orderDate,
                COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            WHERE o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END)
            ORDER BY orderDate
            """, nativeQuery = true)
    List<OrderDailyAmountProjection> findDailyProductAmountByPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * {@link #findDailyProductAmountByPeriod}와 동일하되, {@code payment_method = 'DIRECT'}는 제외한다.
     */
    @Query(value = """
            SELECT
                DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END) AS orderDate,
                COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            WHERE o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND (o.payment_method IS NULL OR o.payment_method <> 'DIRECT')
            GROUP BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END)
            ORDER BY orderDate
            """, nativeQuery = true)
    List<OrderDailyAmountProjection> findDailyProductAmountByPeriodExcludingDirect(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 기간 내 CARD_PRODUCT 주문을 게임·세트 코드별로 집계한다.
     *
     * <p>싱글 카드 온라인 매출 세부에 사용. game별로 1차 그룹하고,
     * set_code별로 종수(categoryCount)·수량(totalQuantity)·금액(totalAmount)을 반환한다.</p>
     */
    @Query(value = """
            SELECT
                psm.game               AS game,
                psm.set_code           AS groupKey,
                COUNT(DISTINCT op.product_id) AS categoryCount,
                COALESCE(SUM(op.quantity), 0) AS totalQuantity,
                COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            LEFT JOIN product_search_maps psm ON op.search_map_id = psm.id
            WHERE op.product_table = 'CARD_PRODUCT'
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY psm.game, psm.set_code
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<OrderOnlineDetailProjection> findOnlineCardProductDetail(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 기간 내 SEALED_PRODUCT 주문을 게임·세트 코드별로 집계한다.
     *
     * <p>밀봉 제품 온라인 매출 세부에 사용. 전체 세트를 금액 내림차순으로 반환한다.</p>
     */
    @Query(value = """
            SELECT
                psm.game               AS game,
                psm.set_code           AS groupKey,
                COUNT(DISTINCT op.product_id) AS categoryCount,
                COALESCE(SUM(op.quantity), 0) AS totalQuantity,
                COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            LEFT JOIN product_search_maps psm ON op.search_map_id = psm.id
            WHERE op.product_table = 'SEALED_PRODUCT'
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY psm.game, psm.set_code
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<OrderOnlineDetailProjection> findOnlineSealedProductDetail(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 기간 내 SUPPLY 주문을 서플라이 타입별로 집계한다.
     *
     * <p>서플라이 온라인 매출 세부에 사용. supplies_type을 groupKey로 반환하며
     * game은 NULL 로 내려온다.</p>
     */
    @Query(value = """
            SELECT
                NULL                   AS game,
                psm.supplies_type      AS groupKey,
                COUNT(DISTINCT op.product_id) AS categoryCount,
                COALESCE(SUM(op.quantity), 0) AS totalQuantity,
                COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            LEFT JOIN product_search_maps psm ON op.search_map_id = psm.id
            WHERE op.product_table = 'SUPPLY'
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY psm.supplies_type
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<OrderOnlineDetailProjection> findOnlineSupplyDetail(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 기간 내 MANUAL_PRODUCT 주문을 수동상품 카테고리별로 집계한다.
     *
     * <p>수동상품 온라인 매출 세부에 사용. manual_category를 groupKey로 반환하며
     * game은 NULL 로 내려온다.</p>
     */
    @Query(value = """
            SELECT
                NULL                   AS game,
                psm.manual_category    AS groupKey,
                COUNT(DISTINCT op.product_id) AS categoryCount,
                COALESCE(SUM(op.quantity), 0) AS totalQuantity,
                COALESCE(SUM(op.total_price), 0) AS totalAmount
            FROM order_products op
            INNER JOIN order_infos o ON op.order_info_id = o.id
            LEFT JOIN product_search_maps psm ON op.search_map_id = psm.id
            WHERE op.product_table = 'MANUAL_PRODUCT'
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
            GROUP BY psm.manual_category
            ORDER BY totalAmount DESC
            """, nativeQuery = true)
    List<OrderOnlineDetailProjection> findOnlineManualDetail(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 비취소 온라인 주문에서 offlineProductId별 출고(주문) 수량을 합산한다.
     *
     * <p>SEALED_PRODUCT / MANUAL_PRODUCT / SUPPLY만 대상. offlineProductId가 비어 있으면 제외.
     * restoreStock=false 취소분은 주문 상태가 CANCELLED라 이 합산에 포함되지 않는다
     * (실시간 델타는 유지하지만 reconcile 절대값 재집계와는 어긋날 수 있음).</p>
     */
    @Query(value = """
            SELECT offline_product_id AS offlineProductId,
                   COALESCE(SUM(quantity), 0) AS totalQuantity
            FROM (
                SELECT sp.offline_product_id AS offline_product_id, op.quantity AS quantity
                FROM order_products op
                INNER JOIN order_infos o ON op.order_info_id = o.id
                INNER JOIN sealed_product sp ON op.product_id = sp.id
                WHERE op.product_table = 'SEALED_PRODUCT'
                  AND o.order_status <> 'ORDER_CANCELLED'
                  AND sp.offline_product_id IS NOT NULL
                  AND sp.offline_product_id <> ''
                UNION ALL
                SELECT mp.offline_product_id AS offline_product_id, op.quantity AS quantity
                FROM order_products op
                INNER JOIN order_infos o ON op.order_info_id = o.id
                INNER JOIN manual_products mp ON op.product_id = mp.id
                WHERE op.product_table = 'MANUAL_PRODUCT'
                  AND o.order_status <> 'ORDER_CANCELLED'
                  AND mp.offline_product_id IS NOT NULL
                  AND mp.offline_product_id <> ''
                UNION ALL
                SELECT s.offline_product_id AS offline_product_id, op.quantity AS quantity
                FROM order_products op
                INNER JOIN order_infos o ON op.order_info_id = o.id
                INNER JOIN supplies s ON op.product_id = s.id
                WHERE op.product_table = 'SUPPLY'
                  AND o.order_status <> 'ORDER_CANCELLED'
                  AND s.offline_product_id IS NOT NULL
                  AND s.offline_product_id <> ''
            ) linked
            GROUP BY offline_product_id
            """, nativeQuery = true)
    List<OfflineProductIdQuantityProjection> sumActiveOnlineQuantityGroupedByOfflineProductId();
}
