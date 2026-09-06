package com.shop.offline.sales.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.offline.sales.dto.projection.OfflineDailyReportPaymentRowProjection;
import com.shop.offline.sales.dto.projection.PaymentSummaryProjection;
import com.shop.offline.sales.entity.OfflineSalesPayment;

public interface OfflineSalesPaymentRepository extends JpaRepository<OfflineSalesPayment, Long> {

    @Modifying
    @Query("DELETE FROM OfflineSalesPayment p WHERE p.offlineSalesInfo.id = :infoId")
    void deleteAllByOfflineSalesInfoId(@Param("infoId") Long infoId);

    /**
     * 지정 날짜 목록의 결제 수단 소스 타입(source_type)별 결제 요약을 조회한다.
     *
     * <p>일별 레포트에서 현금·카드·바코드 등 결제 수단별 건수와 실결제금액을 집계하기 위해 사용한다.
     * 반환된 데이터는 {@link com.shop.admin.analyze.service.OfflineSalesAggregator#aggregateOfflinePayments}에서
     * CASH / CARD_BARCODE / OTHER 3개 그룹으로 분류된다.</p>
     *
     * <p>source_type 예시: CASH, CARD, BARCODE, POINT 등</p>
     */
    @Query(value = """
            SELECT DATE(o.created_at) AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = :orderState
              AND DATE(o.created_at) IN (:periodStarts)
            GROUP BY DATE(o.created_at), p.source_type
            ORDER BY DATE(o.created_at) DESC, p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> dailyPaymentSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts,
            @Param("orderState") String orderState);

    /**
     * 주별 결제 요약을 조회한다.
     * @param periodStarts 기간 시작일
     * @return 주별 결제 요약 데이터
     */
    @Query(value = """
            SELECT DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY) IN (:periodStarts)
            GROUP BY DATE_SUB(DATE(o.created_at), INTERVAL WEEKDAY(o.created_at) DAY), p.source_type
            ORDER BY periodStart DESC, p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> weeklyPaymentSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 월별 결제 요약을 조회한다.
     * @param periodStarts 기간 시작일
     * @return 월별 결제 요약 데이터
     */
    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(
                      CONCAT(YEAR(o.created_at), '-', LPAD(MONTH(o.created_at), 2, '0'), '-01'),
                      '%Y-%m-%d'
                  ) IN (:periodStarts)
            GROUP BY YEAR(o.created_at), MONTH(o.created_at), p.source_type
            ORDER BY YEAR(o.created_at) DESC, MONTH(o.created_at) DESC, p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> monthlyPaymentSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 분기별 결제 요약을 조회한다.
     * @param periodStarts 기간 시작일
     * @return 분기별 결제 요약 데이터
     */
    @Query(value = """
            SELECT STR_TO_DATE(
                       CONCAT(
                           YEAR(o.created_at), '-',
                           LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                       ),
                       '%Y-%m-%d'
                   ) AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(
                      CONCAT(
                          YEAR(o.created_at), '-',
                          LPAD((QUARTER(o.created_at) - 1) * 3 + 1, 2, '0'), '-01'
                      ),
                      '%Y-%m-%d'
                  ) IN (:periodStarts)
            GROUP BY YEAR(o.created_at), QUARTER(o.created_at), p.source_type
            ORDER BY YEAR(o.created_at) DESC, QUARTER(o.created_at) DESC, p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> quarterlyPaymentSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 연별 결제 요약을 조회한다.
     * @param periodStarts 기간 시작일
     * @return 연별 결제 요약 데이터
     */
    @Query(value = """
            SELECT STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
              AND STR_TO_DATE(CONCAT(YEAR(o.created_at), '-01-01'), '%Y-%m-%d') IN (:periodStarts)
            GROUP BY YEAR(o.created_at), p.source_type
            ORDER BY YEAR(o.created_at) DESC, p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> yearlyPaymentSummaryByPeriodStarts(
            @Param("periodStarts") List<LocalDate> periodStarts);

    /**
     * 전체 결제 요약을 조회한다.
     * @return 전체 결제 요약 데이터
     */
    @Query(value = """
            SELECT NULL AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = 'COMPLETED'
            GROUP BY p.source_type
            ORDER BY p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> totalPaymentSummary();

    /**
     * 특정 날짜의 오프라인 결제 행 데이터를 조회한다. (금액 배분 계산용)
     *
     * <p>한 주문에 여러 결제 수단이 사용된 경우, 결제 수단당 1행이 반환된다.
     * 이 데이터를 바탕으로 {@link com.shop.admin.analyze.service.OfflineSalesAggregator#allocateOfflinePaymentAmounts}가
     * 결제 비율에 따라 정가·할인액을 각 결제 그룹에 배분한다.</p>
     *
     * <p>반환 컬럼:
     * <ul>
     *   <li>listPrice: 주문의 정가 합계 (할인 전 금액)</li>
     *   <li>discountAmount: 주문 헤더 레벨 할인 금액</li>
     *   <li>itemDiscountAmount: 아이템 단위 할인 합계 (없으면 0) — discountAmount보다 우선 사용됨</li>
     *   <li>totalAmount: 주문 실제 총금액 (결제 비율 계산의 분모)</li>
     *   <li>sourceType: 이 결제 수단의 타입 (CASH, CARD, BARCODE 등)</li>
     *   <li>paymentAmount: 이 결제 수단이 실제로 낸 금액 (결제 비율 계산의 분자)</li>
     * </ul>
     * </p>
     */
    @Query(value = """
            SELECT o.id AS orderInfoId,
                   COALESCE(o.list_price, 0) AS listPrice,
                   COALESCE(o.discount_amount, 0) AS discountAmount,
                   COALESCE(o.total_amount, 0) AS totalAmount,
                   COALESCE(item_discounts.item_discount_amount, 0) AS itemDiscountAmount,
                   p.source_type AS sourceType,
                   COALESCE(p.amount, 0) AS paymentAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT i.offline_sales_info_id AS offline_sales_info_id,
                       COALESCE(SUM(d.amount), 0) AS item_discount_amount
                FROM offline_sales_items i
                INNER JOIN offline_sales_infos completed_order ON completed_order.id = i.offline_sales_info_id
                LEFT JOIN offline_sales_item_discounts d ON d.offline_sales_item_id = i.id
                WHERE completed_order.order_state = :orderState
                GROUP BY i.offline_sales_info_id
            ) item_discounts ON item_discounts.offline_sales_info_id = o.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :day
              AND o.created_at < :day + INTERVAL 1 DAY
            """,
            nativeQuery = true)
    List<OfflineDailyReportPaymentRowProjection> findDailyReportPaymentRows(
            @Param("day") LocalDate day,
            @Param("orderState") String orderState);

    /**
     * 지정 기간(시작~종료) 범위의 오프라인 결제 행 데이터를 조회한다. (주간·월간 보고서용)
     */
    @Query(value = """
            SELECT o.id AS orderInfoId,
                   COALESCE(o.list_price, 0) AS listPrice,
                   COALESCE(o.discount_amount, 0) AS discountAmount,
                   COALESCE(o.total_amount, 0) AS totalAmount,
                   COALESCE(item_discounts.item_discount_amount, 0) AS itemDiscountAmount,
                   p.source_type AS sourceType,
                   COALESCE(p.amount, 0) AS paymentAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            LEFT JOIN (
                SELECT i.offline_sales_info_id AS offline_sales_info_id,
                       COALESCE(SUM(d.amount), 0) AS item_discount_amount
                FROM offline_sales_items i
                INNER JOIN offline_sales_infos completed_order ON completed_order.id = i.offline_sales_info_id
                LEFT JOIN offline_sales_item_discounts d ON d.offline_sales_item_id = i.id
                WHERE completed_order.order_state = :orderState
                GROUP BY i.offline_sales_info_id
            ) item_discounts ON item_discounts.offline_sales_info_id = o.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
            """,
            nativeQuery = true)
    List<OfflineDailyReportPaymentRowProjection> findReportPaymentRowsByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState);

    /**
     * 지정 기간(시작~종료) 범위의 결제 수단 소스 타입별 결제 요약을 조회한다. (주간·월간 보고서용)
     */
    @Query(value = """
            SELECT NULL AS periodStart,
                   p.source_type AS sourceType,
                   COUNT(*) AS paymentCount,
                   COALESCE(SUM(p.amount), 0) AS totalAmount,
                   COALESCE(SUM(p.tax_amount), 0) AS totalTaxAmount,
                   COALESCE(SUM(p.supply_amount), 0) AS totalSupplyAmount,
                   COALESCE(SUM(p.tax_exempt_amount), 0) AS totalTaxExemptAmount
            FROM offline_sales_payments p
            INNER JOIN offline_sales_infos o ON p.offline_sales_info_id = o.id
            WHERE o.order_state = :orderState
              AND o.created_at >= :startDate
              AND o.created_at < :endDate
            GROUP BY p.source_type
            ORDER BY p.source_type
            """,
            nativeQuery = true)
    List<PaymentSummaryProjection> findPaymentSummaryByPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("orderState") String orderState);
}
