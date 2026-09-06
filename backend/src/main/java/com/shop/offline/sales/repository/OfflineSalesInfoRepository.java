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

import com.shop.offline.sales.dto.projection.SalesSummaryProjection;
import com.shop.offline.sales.entity.OfflineSalesInfo;

public interface OfflineSalesInfoRepository extends JpaRepository<OfflineSalesInfo, Long> {

    Optional<OfflineSalesInfo> findFirstByOrderIdOrderByIdAsc(String orderId);

    List<OfflineSalesInfo> findAllByOrderId(String orderId);

    @Query(value = """
            SELECT DATE(o.created_at) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            GROUP BY DATE(o.created_at)
            ORDER BY DATE(o.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT DATE(o.created_at))
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            """,
            nativeQuery = true)
    Page<SalesSummaryProjection> dailySalesSummaryAndCompleted(Pageable pageable);

    @Query(value = """
            SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            GROUP BY DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY)
            ORDER BY periodStart DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY))
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            """,
            nativeQuery = true)
    Page<SalesSummaryProjection> weeklySalesSummaryAndCompleted(Pageable pageable);

    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            GROUP BY YEAR(o.created_at), MONTH(o.created_at)
            ORDER BY YEAR(o.created_at) DESC, MONTH(o.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT CONCAT(YEAR(o.created_at), '-', MONTH(o.created_at)))
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            """,
            nativeQuery = true)
    Page<SalesSummaryProjection> monthlySalesSummaryAndCompleted(Pageable pageable);

    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(
                           YEAR(o.created_at), '-',
                           LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                       ),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            GROUP BY YEAR(o.created_at), QUARTER(o.created_at)
            ORDER BY YEAR(o.created_at) DESC, QUARTER(o.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT CONCAT(YEAR(o.created_at), '-Q', QUARTER(o.created_at)))
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            """,
            nativeQuery = true)
    Page<SalesSummaryProjection> quarterlySalesSummaryAndCompleted(Pageable pageable);

    @Query(value = """
            SELECT STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            GROUP BY YEAR(o.created_at)
            ORDER BY YEAR(o.created_at) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT YEAR(o.created_at))
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            """,
            nativeQuery = true)
    Page<SalesSummaryProjection> yearlySalesSummaryAndCompleted(Pageable pageable);

    @Query(value = """
            SELECT NULL AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
            """,
            countQuery = """
            SELECT 1
            """,
            nativeQuery = true)
    Page<SalesSummaryProjection> totalSalesSummaryAndCompleted(Pageable pageable);

    /**
     * 특정 날짜의 오프라인 판매 요약 1건을 조회한다.
     *
     * <p>일별 레포트에서 오프라인 전체 주문 건수·정가 합계를 가져오기 위해 사용한다.
     * 결과는 Optional로 반환되며, 해당 날짜에 완료된 오프라인 주문이 없으면 empty가 된다.</p>
     *
     * <p>날짜 기준: offline_sales_infos.created_at (주문 생성 시각)</p>
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = :orderState
              AND o.created_at >= :periodStart
              AND o.created_at < :periodStart + INTERVAL 1 DAY
            GROUP BY DATE(o.created_at)
            """,
            nativeQuery = true)
    Optional<SalesSummaryProjection> dailySalesSummaryByPeriodStart(
            @Param("periodStart") LocalDate periodStart,
            @Param("orderState") String orderState);

    @Query(value = """
            SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
              AND DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) = :periodStart
            GROUP BY DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY)
            """,
            nativeQuery = true)
    Optional<SalesSummaryProjection> weeklySalesSummaryByPeriodStart(
            @Param("periodStart") LocalDate periodStart);

    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(
                      CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                      '%Y-%m-%d'
                  ) = :periodStart
            GROUP BY YEAR(o.created_at), MONTH(o.created_at)
            """,
            nativeQuery = true)
    Optional<SalesSummaryProjection> monthlySalesSummaryByPeriodStart(
            @Param("periodStart") LocalDate periodStart);

    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(
                           YEAR(o.created_at), '-',
                           LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                       ),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(
                      CONCAT(
                          YEAR(o.created_at), '-',
                          LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                      ),
                      '%Y-%m-%d'
                  ) = :periodStart
            GROUP BY YEAR(o.created_at), QUARTER(o.created_at)
            """,
            nativeQuery = true)
    Optional<SalesSummaryProjection> quarterlySalesSummaryByPeriodStart(
            @Param("periodStart") LocalDate periodStart);

    @Query(value = """
            SELECT STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') = :periodStart
            GROUP BY YEAR(o.created_at)
            """,
            nativeQuery = true)
    Optional<SalesSummaryProjection> yearlySalesSummaryByPeriodStart(
            @Param("periodStart") LocalDate periodStart);

    @Query(value = """
            SELECT o FROM OfflineSalesInfo o
            WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate
            """)
    Page<OfflineSalesInfo> findAllByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * 지정 기간(시작~종료) 범위의 오프라인 판매 요약을 조회한다. (주간·월간 보고서용)
     */
    @Query(value = """
            SELECT NULL AS periodStart,
                   COUNT(*) AS totalOrderCount,
                   COALESCE(SUM(o.list_price), 0) AS totalOrderAmount,
                   COALESCE(SUM(o.discount_amount), 0) AS totalDiscountAmount,
                   COALESCE(SUM(o.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(o.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(o.tax_exempt_amount), 0) AS totalTaxExemptAmount,
                   COALESCE(SUM(o.total_amount), 0) AS totalTotalAmount
            FROM offline_sales_infos o
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
            """,
            nativeQuery = true)
    Optional<SalesSummaryProjection> findSalesSummaryByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState);
}
