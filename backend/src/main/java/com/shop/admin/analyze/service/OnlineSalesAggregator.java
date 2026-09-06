package com.shop.admin.analyze.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shop.admin.analyze.dto.AdminWeeklyReportDto.CardProductSaleDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.ManualProductSaleDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.ManualProductSalesSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SealedProductSaleDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SealedProductSalesSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SingleCardSalesSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SupplyProductSaleDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SupplyProductSalesSummaryDto;
import com.shop.order.dto.projection.OrderDailyReportByPaymentMethodProjection;
import com.shop.order.dto.projection.OrderDailyReportByProductTableProjection;
import com.shop.order.dto.projection.OrderOnlineDetailProjection;

/**
 * 온라인 주문 데이터 집계 담당.
 *
 * <p>Repository에서 받은 raw 프로젝션을 결제 수단별·상품 테이블별로 합산해
 * AdminDailyReportDto에 채울 수 있는 형태로 변환한다.</p>
 */
@Component
class OnlineSalesAggregator {

    /** 결제 방법 = 직접 결제 (매장결제) */
    static final String PAYMENT_GROUP_DIRECT = "DIRECT";
    /** 결제 방법 = 온라인 카드 결제 */
    static final String PAYMENT_GROUP_ONLINE_CARD = "ONLINE_CARD";
    /** 결제 방법 = 온라인 간편결제 */
    static final String PAYMENT_GROUP_ONLINE_EASY_PAY = "ONLINE_EASY_PAY";
    /** 결제 방법 = 그 외 (TOSS 폴백, 가상계좌 등). 화면 미표시, 총계에만 포함 */
    static final String PAYMENT_GROUP_ONLINE_OTHER = "ONLINE_OTHER";

    /**
     * 결제 수단 그룹별 온라인 주문 집계값.
     *
     * @param orderCount          주문 건수
     * @param orderAmount         상품 합계 금액 (order_products.total_price 합산)
     * @param usedPointAmount     사용 포인트 금액
     * @param actualPaymentAmount 실제 결제 금액 (포인트 차감 후)
     */
    record PaymentMethodAggregate(long orderCount, long orderAmount, long usedPointAmount, long actualPaymentAmount) {
        static PaymentMethodAggregate empty() {
            return new PaymentMethodAggregate(0L, 0L, 0L, 0L);
        }
    }

    /**
     * 상품 테이블(종류)별 온라인 주문 집계값.
     *
     * <p>CARD_PRODUCT → 싱글 카드, SEALED_PRODUCT → 봉입 상품,
     * SUPPLY → 용품, 그 외(MANUAL_PRODUCT / UNION_PRICE / OTHER_PRODUCT 등) → 기타</p>
     */
    record ProductTableAggregates(
            long singleCardCategoryCount, long singleCardQuantity, long singleCardAmount,
            long sealedCategoryCount, long sealedQuantity, long sealedAmount,
            long supplyCategoryCount, long supplyQuantity, long supplyAmount,
            long otherCategoryCount, long otherQuantity, long otherAmount) {
    }

    /**
     * 결제 수단별 주문 집계를 두 쿼리 결과를 합쳐서 완성한다.
     *
     * <p>order_infos 테이블(headers)은 주문 건수·포인트·실결제금액을 가지고 있고,
     * order_products 테이블(lines)은 상품별 금액 합계(orderAmount)를 가지고 있다.
     * 두 쿼리를 결제 그룹(paymentGroup) 키로 병합해야 모든 필드를 채울 수 있다.</p>
     *
     * @param headers order_infos 기준 집계 (건수·포인트·실결제금액 포함)
     * @param lines   order_products 기준 집계 (상품 합계 금액 포함)
     * @return 결제 그룹 → 집계값 맵
     */
    Map<String, PaymentMethodAggregate> mergeOnlinePaymentAggregates(
            List<OrderDailyReportByPaymentMethodProjection> headers,
            List<OrderDailyReportByPaymentMethodProjection> lines) {

        Map<String, PaymentMethodAggregate> result = new HashMap<>();

        // 1단계: headers에서 건수·포인트·실결제금액 먼저 채운다. orderAmount는 아직 0.
        for (OrderDailyReportByPaymentMethodProjection header : headers) {
            result.put(header.getPaymentGroup(), new PaymentMethodAggregate(
                    nullToZero(header.getOrderCount()),
                    0L,
                    toLong(header.getUsedPointAmount()),
                    toLong(header.getActualPaymentAmount())));
        }

        // 2단계: lines에서 상품 합계 금액(orderAmount)을 덮어쓴다.
        // 건수는 headers에서 이미 채워졌으면 유지하고, 없을 때만 lines 값으로 채운다.
        for (OrderDailyReportByPaymentMethodProjection line : lines) {
            PaymentMethodAggregate existing = result.getOrDefault(line.getPaymentGroup(), PaymentMethodAggregate.empty());
            result.put(line.getPaymentGroup(), new PaymentMethodAggregate(
                    existing.orderCount() > 0 ? existing.orderCount() : nullToZero(line.getOrderCount()),
                    toLong(line.getOrderAmount()),
                    existing.usedPointAmount(),
                    existing.actualPaymentAmount()));
        }

        return result;
    }

    /**
     * 상품 테이블(DB의 product_table 컬럼)별로 온라인 주문 수량·금액을 분류·합산한다.
     *
     * <p>product_table 값 → 분류:
     * <ul>
     *   <li>CARD_PRODUCT → 싱글 카드</li>
     *   <li>SEALED_PRODUCT → 봉입 상품</li>
     *   <li>SUPPLY → 용품</li>
     *   <li>MANUAL_PRODUCT, UNION_PRICE, OTHER_PRODUCT, 그 외 → 기타</li>
     * </ul>
     * </p>
     *
     * @param rows product_table별 집계 프로젝션 목록
     * @return 분류별 집계값
     */
    ProductTableAggregates aggregateProductTable(List<OrderDailyReportByProductTableProjection> rows) {
        long singleCardCategoryCount = 0L;
        long singleCardQuantity = 0L;
        long singleCardAmount = 0L;
        long sealedCategoryCount = 0L;
        long sealedQuantity = 0L;
        long sealedAmount = 0L;
        long supplyCategoryCount = 0L;
        long supplyQuantity = 0L;
        long supplyAmount = 0L;
        long otherCategoryCount = 0L;
        long otherQuantity = 0L;
        long otherAmount = 0L;

        for (OrderDailyReportByProductTableProjection row : rows) {
            String productTable = row.getProductTable();
            long categoryCount = nullToZero(row.getCategoryCount());
            long quantity = nullToZero(row.getQuantity());
            long amount = toLong(row.getOrderAmount());

            switch (productTable) {
                case "CARD_PRODUCT" -> {
                    singleCardCategoryCount += categoryCount;
                    singleCardQuantity += quantity;
                    singleCardAmount += amount;
                }
                case "SEALED_PRODUCT" -> {
                    sealedCategoryCount += categoryCount;
                    sealedQuantity += quantity;
                    sealedAmount += amount;
                }
                case "SUPPLY" -> {
                    supplyCategoryCount += categoryCount;
                    supplyQuantity += quantity;
                    supplyAmount += amount;
                }
                // MANUAL_PRODUCT, UNION_PRICE, OTHER_PRODUCT 및 미분류는 모두 기타로 처리
                default -> {
                    otherCategoryCount += categoryCount;
                    otherQuantity += quantity;
                    otherAmount += amount;
                }
            }
        }

        return new ProductTableAggregates(
                singleCardCategoryCount, singleCardQuantity, singleCardAmount,
                sealedCategoryCount, sealedQuantity, sealedAmount,
                supplyCategoryCount, supplyQuantity, supplyAmount,
                otherCategoryCount, otherQuantity, otherAmount);
    }

    /**
     * CARD_PRODUCT 집계 결과를 게임별 싱글 카드 요약 리스트로 변환한다.
     *
     * <p>각 게임 내에서 금액 기준 상위 3개 세트는 개별 항목으로,
     * 나머지는 "기타"로 합산한다. 비중은 게임 내 전체 금액 대비 %.</p>
     */
    List<SingleCardSalesSummaryDto> aggregateSingleCardDetails(List<OrderOnlineDetailProjection> rows) {
        Map<String, List<OrderOnlineDetailProjection>> byGame = groupByGame(rows);
        List<SingleCardSalesSummaryDto> result = new ArrayList<>();

        for (Map.Entry<String, List<OrderOnlineDetailProjection>> entry : byGame.entrySet()) {
            String game = entry.getKey();
            List<OrderOnlineDetailProjection> gameRows = entry.getValue();

            long gameTotal = gameRows.stream().mapToLong(r -> toLong(r.getTotalAmount())).sum();

            List<CardProductSaleDto> saleDtos = new ArrayList<>();
            List<OrderOnlineDetailProjection> top3 = gameRows.subList(0, Math.min(3, gameRows.size()));
            List<OrderOnlineDetailProjection> rest = gameRows.subList(Math.min(3, gameRows.size()), gameRows.size());

            for (OrderOnlineDetailProjection r : top3) {
                long amount = toLong(r.getTotalAmount());
                saleDtos.add(CardProductSaleDto.builder()
                        .setName(r.getGroupKey())
                        .totalSalesCount(nullToZero(r.getCategoryCount()))
                        .totalSalesQuantity(nullToZero(r.getTotalQuantity()))
                        .totalSalesAmount(amount)
                        .totalSalesPercentage(gameTotal > 0 ? (double) amount / gameTotal * 100.0 : null)
                        .build());
            }

            if (!rest.isEmpty()) {
                long etcCount = rest.stream().mapToLong(r -> nullToZero(r.getCategoryCount())).sum();
                long etcQty = rest.stream().mapToLong(r -> nullToZero(r.getTotalQuantity())).sum();
                long etcAmount = rest.stream().mapToLong(r -> toLong(r.getTotalAmount())).sum();
                saleDtos.add(CardProductSaleDto.builder()
                        .setName("기타")
                        .totalSalesCount(etcCount)
                        .totalSalesQuantity(etcQty)
                        .totalSalesAmount(etcAmount)
                        .totalSalesPercentage(gameTotal > 0 ? (double) etcAmount / gameTotal * 100.0 : null)
                        .build());
            }

            result.add(SingleCardSalesSummaryDto.builder()
                    .game(game)
                    .cardProductSaleDtoList(saleDtos)
                    .build());
        }
        return result;
    }

    /**
     * SEALED_PRODUCT 집계 결과를 게임별 밀봉 제품 요약 리스트로 변환한다.
     *
     * <p>각 게임 내 전체 세트를 금액 내림차순으로 표시한다.
     * 비중은 게임 내 전체 금액 대비 %.</p>
     */
    List<SealedProductSalesSummaryDto> aggregateSealedProductDetails(List<OrderOnlineDetailProjection> rows) {
        Map<String, List<OrderOnlineDetailProjection>> byGame = groupByGame(rows);
        List<SealedProductSalesSummaryDto> result = new ArrayList<>();

        for (Map.Entry<String, List<OrderOnlineDetailProjection>> entry : byGame.entrySet()) {
            String game = entry.getKey();
            List<OrderOnlineDetailProjection> gameRows = entry.getValue();

            long gameTotal = gameRows.stream().mapToLong(r -> toLong(r.getTotalAmount())).sum();

            List<SealedProductSaleDto> saleDtos = new ArrayList<>();
            for (OrderOnlineDetailProjection r : gameRows) {
                long amount = toLong(r.getTotalAmount());
                saleDtos.add(SealedProductSaleDto.builder()
                        .setName(r.getGroupKey())
                        .totalSalesCount(nullToZero(r.getCategoryCount()))
                        .totalSalesQuantity(nullToZero(r.getTotalQuantity()))
                        .totalSalesAmount(amount)
                        .totalSalesPercentage(gameTotal > 0 ? (double) amount / gameTotal * 100.0 : null)
                        .build());
            }

            result.add(SealedProductSalesSummaryDto.builder()
                    .game(game)
                    .sealedProductSaleDtoList(saleDtos)
                    .build());
        }
        return result;
    }

    /**
     * SUPPLY 집계 결과를 서플라이 타입별 요약으로 변환한다.
     *
     * <p>전체를 금액 내림차순으로 표시. 비중은 서플라이 전체 금액 대비 %.</p>
     */
    SupplyProductSalesSummaryDto aggregateSupplyDetails(List<OrderOnlineDetailProjection> rows) {
        long total = rows.stream().mapToLong(r -> toLong(r.getTotalAmount())).sum();
        List<SupplyProductSaleDto> saleDtos = new ArrayList<>();
        for (OrderOnlineDetailProjection r : rows) {
            long amount = toLong(r.getTotalAmount());
            saleDtos.add(SupplyProductSaleDto.builder()
                    .supplyType(r.getGroupKey())
                    .totalSalesCount(nullToZero(r.getCategoryCount()))
                    .totalSalesQuantity(nullToZero(r.getTotalQuantity()))
                    .totalSalesAmount(amount)
                    .totalSalesPercentage(total > 0 ? (double) amount / total * 100.0 : null)
                    .build());
        }
        return SupplyProductSalesSummaryDto.builder()
                .supplyProductSaleDtoList(saleDtos)
                .build();
    }

    /**
     * MANUAL_PRODUCT 집계 결과를 수동상품 카테고리별 요약으로 변환한다.
     *
     * <p>전체를 금액 내림차순으로 표시. 비중은 수동상품 전체 금액 대비 %.</p>
     */
    ManualProductSalesSummaryDto aggregateManualDetails(List<OrderOnlineDetailProjection> rows) {
        long total = rows.stream().mapToLong(r -> toLong(r.getTotalAmount())).sum();
        List<ManualProductSaleDto> saleDtos = new ArrayList<>();
        for (OrderOnlineDetailProjection r : rows) {
            long amount = toLong(r.getTotalAmount());
            saleDtos.add(ManualProductSaleDto.builder()
                    .manualType(r.getGroupKey())
                    .totalSalesCount(nullToZero(r.getCategoryCount()))
                    .totalSalesQuantity(nullToZero(r.getTotalQuantity()))
                    .totalSalesAmount(amount)
                    .totalSalesPercentage(total > 0 ? (double) amount / total * 100.0 : null)
                    .build());
        }
        return ManualProductSalesSummaryDto.builder()
                .manualProductSaleDtoList(saleDtos)
                .build();
    }

    /**
     * 프로젝션 목록을 game 키로 그룹화하고, 각 그룹 내에서 금액 내림차순으로 정렬한다.
     * game이 null인 행은 "(미분류)"로 처리한다.
     */
    private Map<String, List<OrderOnlineDetailProjection>> groupByGame(List<OrderOnlineDetailProjection> rows) {
        Map<String, List<OrderOnlineDetailProjection>> map = new LinkedHashMap<>();
        for (OrderOnlineDetailProjection r : rows) {
            String game = r.getGame() != null ? r.getGame() : "(미분류)";
            map.computeIfAbsent(game, k -> new ArrayList<>()).add(r);
        }
        map.forEach((game, list) ->
                list.sort(Comparator.comparingLong((OrderOnlineDetailProjection r) -> toLong(r.getTotalAmount())).reversed()));
        return map;
    }

    private long toLong(BigDecimal value) {
        return value != null ? value.longValue() : 0L;
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
