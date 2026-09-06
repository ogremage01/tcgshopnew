package com.shop.order.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.order.dto.projection.OrderDailyAmountProjection;
import com.shop.order.dto.projection.OrderDailyDeliveryProjection;
import com.shop.order.dto.projection.OrderDailyReportByPaymentMethodProjection;
import com.shop.order.dto.projection.OrderDailySalesSummaryProjection;
import com.shop.order.entity.OrderInfo;

public interface OrderInfoRepository extends JpaRepository<OrderInfo, Long>, OrderInfoRepositoryCustom {

    Page<OrderInfo> findByUserIdOrderByPaymentDateDesc(Long userId, Pageable pageable);

    /**
     * 목록 필터와 동일: {@code paymentDate} 우선, 없으면 {@code paymentApprovedAt}.
     */
    @Query("""
            SELECT o FROM OrderInfo o
            WHERE (o.paymentDate IS NOT NULL AND o.paymentDate >= :start AND o.paymentDate <= :end)
               OR (o.paymentDate IS NULL AND o.paymentApprovedAt IS NOT NULL
                   AND o.paymentApprovedAt >= :start AND o.paymentApprovedAt <= :end)
            """)
    List<OrderInfo> findByEffectiveOrderAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByOrderStatus(String orderStatus);

    /**
     * 지정 기간의 일자별 온라인 주문 판매 요약을 조회한다.
     *
     * <p>날짜 기준: payment_date가 있으면 그것을, 없으면 payment_approved_at을 사용한다.
     * 대상: ORDER_DELIVERY_COMPLETED(배송 완료) 상태인 주문만 포함한다.</p>
     *
     * <p>반환 컬럼:
     * <ul>
     *   <li>orderDate: 일자</li>
     *   <li>totalOrderCount: 주문 건수</li>
     *   <li>totalOrderAmount: 상품 금액 합계 (total_product_amount)</li>
     *   <li>totalUsedPointAmount: 사용 포인트 합계</li>
     *   <li>totalDeliveryFee: 배송비 합계</li>
     *   <li>totalActualPaymentAmount: 실결제금액 합계 (포인트 차감 후)</li>
     * </ul>
     * </p>
     */
    @Query(value = """
            SELECT
                DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END) AS orderDate,
                COUNT(*)                          AS totalOrderCount,
                COALESCE(SUM(o.total_product_amount), 0)   AS totalOrderAmount,
                COALESCE(SUM(o.used_point_amount), 0)      AS totalUsedPointAmount,
                COALESCE(SUM(o.delivery_fee), 0)           AS totalDeliveryFee,
                COALESCE(SUM(o.actual_payment_amount), 0)  AS totalActualPaymentAmount
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
            GROUP BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END)
            ORDER BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END))
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
            """,
            nativeQuery = true)
    Page<OrderDailySalesSummaryProjection> findSalesSummaryByPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    /**
     * {@link #findSalesSummaryByPeriod}와 동일하되, 매장 직접결제({@code payment_method = 'DIRECT'})는 제외한다.
     *
     * <p>DIRECT 주문은 오프라인 매출에도 집계되므로 온라인·오프라인 합산 목록에서
     * 이중 합산을 막기 위해 사용한다.</p>
     */
    @Query(value = """
            SELECT
                DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END) AS orderDate,
                COUNT(*)                          AS totalOrderCount,
                COALESCE(SUM(o.total_product_amount), 0)   AS totalOrderAmount,
                COALESCE(SUM(o.used_point_amount), 0)      AS totalUsedPointAmount,
                COALESCE(SUM(o.delivery_fee), 0)           AS totalDeliveryFee,
                COALESCE(SUM(o.actual_payment_amount), 0)  AS totalActualPaymentAmount
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
              AND (o.payment_method IS NULL OR o.payment_method <> 'DIRECT')
            GROUP BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END)
            ORDER BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END)
            """, nativeQuery = true)
    List<OrderDailySalesSummaryProjection> findSalesSummaryByPeriodExcludingDirect(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 지정 기간의 온라인 주문 판매 요약 합계를 단일 행으로 조회한다.
     *
     * <p>{@link #findSalesSummaryByPeriod}와 조건은 동일하나, 일자별 GROUP BY 없이
     * 전체 합계만 필요할 때 사용한다(당월/전체 매출 요약). 집계 함수만 사용하므로
     * 항상 1행이 반환되며, 데이터가 없으면 count 0 / 합계 0으로 내려온다.</p>
     */
    @Query(value = """
            SELECT
                NULL                                       AS orderDate,
                COUNT(*)                                   AS totalOrderCount,
                COALESCE(SUM(o.total_product_amount), 0)   AS totalOrderAmount,
                COALESCE(SUM(o.used_point_amount), 0)      AS totalUsedPointAmount,
                COALESCE(SUM(o.delivery_fee), 0)           AS totalDeliveryFee,
                COALESCE(SUM(o.actual_payment_amount), 0)  AS totalActualPaymentAmount
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
            """, nativeQuery = true)
    OrderDailySalesSummaryProjection findSalesTotalsByPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 지정 기간의 결제 수단 그룹별 주문 집계를 조회한다. (order_infos 기준)
     *
     * <p>결제 수단 그룹: DIRECT(매장) / ONLINE_CARD(카드) / ONLINE_EASY_PAY(간편결제) / ONLINE_OTHER(그 외).
     * 이 쿼리는 주문 건수·포인트·실결제금액을 반환하며,
     * 상품 합계 금액(orderAmount)은 order_products 기준 쿼리
     * ({@link OrderProductRepository#findDailyReportLineAmountByPaymentMethod})와 합산해서 사용한다.</p>
     */
    @Query(value = """
            SELECT
                CASE
                    WHEN o.payment_method = 'DIRECT' THEN 'DIRECT'
                    WHEN o.payment_method = '카드' THEN 'ONLINE_CARD'
                    WHEN o.payment_method = '간편결제' THEN 'ONLINE_EASY_PAY'
                    ELSE 'ONLINE_OTHER'
                END AS paymentGroup,
                COUNT(*) AS orderCount,
                COALESCE(SUM(o.total_product_amount), 0) AS orderAmount,
                COALESCE(SUM(o.used_point_amount), 0) AS usedPointAmount,
                COALESCE(SUM(o.actual_payment_amount), 0) AS actualPaymentAmount
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
            GROUP BY CASE
                WHEN o.payment_method = 'DIRECT' THEN 'DIRECT'
                WHEN o.payment_method = '카드' THEN 'ONLINE_CARD'
                WHEN o.payment_method = '간편결제' THEN 'ONLINE_EASY_PAY'
                ELSE 'ONLINE_OTHER'
            END
            """, nativeQuery = true)
    List<OrderDailyReportByPaymentMethodProjection> findDailyReportByPaymentMethod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 지정 기간의 온라인 배송 건수·배송료 합계를 조회한다.
     *
     * <p>배송 건수: delivery_fee &gt; 0 인 주문 수.
     * 배송료 합계: delivery_fee 전체 합산.</p>
     */
    @Query(value = """
            SELECT
                COUNT(CASE WHEN o.delivery_fee > 0 THEN 1 END) AS deliveryCount,
                COALESCE(SUM(o.delivery_fee), 0)               AS deliveryFeeSum
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
            """, nativeQuery = true)
    OrderDailyDeliveryProjection findDailyDelivery(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 지정 기간의 일자별 온라인 배송료 합계를 조회한다.
     */
    @Query(value = """
            SELECT
                DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END) AS orderDate,
                COALESCE(SUM(o.delivery_fee), 0) AS totalAmount
            FROM order_infos o
            WHERE ((o.payment_date IS NOT NULL AND o.payment_date >= :start AND o.payment_date < :end)
                OR (o.payment_date IS NULL AND o.payment_approved_at IS NOT NULL
                    AND o.payment_approved_at >= :start AND o.payment_approved_at < :end))
              AND o.order_status = 'ORDER_DELIVERY_COMPLETED'
            GROUP BY DATE(CASE WHEN o.payment_date IS NOT NULL THEN o.payment_date ELSE o.payment_approved_at END)
            ORDER BY orderDate
            """, nativeQuery = true)
    List<OrderDailyAmountProjection> findDailyDeliveryFeeByPeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

}
