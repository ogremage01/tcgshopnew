package com.shop.admin.analyze.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shop.admin.analyze.dto.AdminWeeklyReportDto.ManualProductSaleDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.OfflineSalesSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SealedProductSaleDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SealedProductSalesSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto.SupplyProductSaleDto;
import com.shop.offline.sales.dto.projection.OfflineDailyReportByLinkTableProjection;
import com.shop.offline.sales.dto.projection.OfflineDailyReportPaymentRowProjection;
import com.shop.offline.sales.dto.projection.OfflineDetailByLinkTableProjection;
import com.shop.offline.sales.dto.projection.PaymentSummaryProjection;

/**
 * 오프라인 판매 데이터 집계 담당.
 *
 * <p>오프라인 판매는 한 건의 주문에 현금·카드 등 복수의 결제 수단이 섞일 수 있어,
 * 결제 비율로 금액을 배분하는 로직이 핵심이다.</p>
 */
@Component
class OfflineSalesAggregator {

    /** 오프라인 결제 그룹: 현금 */
    private static final String OFFLINE_GROUP_CASH = "CASH";
    /** 오프라인 결제 그룹: 카드 / 바코드 */
    private static final String OFFLINE_GROUP_CARD_BARCODE = "CARD_BARCODE";
    /** 오프라인 결제 그룹: 기타 (포인트, 쿠폰 등) */
    private static final String OFFLINE_GROUP_OTHER = "OTHER";

    /**
     * 결제 그룹별 오프라인 결제 집계값.
     *
     * <p>grossAmount(정가 합계)와 discountAmount(할인 합계)는
     * {@link #allocateOfflinePaymentAmounts}를 호출한 뒤에야 채워진다.
     * {@link #aggregateOfflinePayments}만 호출한 시점에는 0이다.</p>
     *
     * @param paymentCount  결제 건수
     * @param grossAmount   정가 합계 (allocateOfflinePaymentAmounts 이후 채워짐)
     * @param discountAmount 할인 금액 합계 (allocateOfflinePaymentAmounts 이후 채워짐)
     * @param paymentAmount 실 결제 금액 합계
     */
    record OfflinePaymentGroupAggregate(long paymentCount, long grossAmount, long discountAmount, long paymentAmount) {
        static OfflinePaymentGroupAggregate empty() {
            return new OfflinePaymentGroupAggregate(0L, 0L, 0L, 0L);
        }
    }

    /**
     * 링크 테이블(상품 분류) 단위 오프라인 판매 집계값.
     *
     * @param categoryCount  고유 상품 종류 수
     * @param quantity       판매 수량 합계
     * @param grossAmount    정가(판매가) 합계
     * @param discountAmount 할인 금액 합계
     * @param paymentAmount  실 결제 금액 합계 (grossAmount - discountAmount)
     */
    record LinkTableProductAggregate(long categoryCount, long quantity, long grossAmount, long discountAmount, long paymentAmount) {
        static LinkTableProductAggregate empty() {
            return new LinkTableProductAggregate(0L, 0L, 0L, 0L, 0L);
        }
    }

    /**
     * 링크 테이블 분류별 집계 + 전체 합계.
     *
     * <p>링크 테이블: Sealed(봉입 상품), Supply(용품), Manual(직접 입력 등 나머지)</p>
     *
     * @param sealed       봉입 상품 집계
     * @param supply       용품 집계
     * @param manual       기타(직접 입력 등) 집계
     * @param totalGross   전체 정가 합계
     * @param totalDiscount 전체 할인 합계
     * @param totalPayment 전체 실결제 합계
     */
    record LinkTableAggregates(
            LinkTableProductAggregate sealed,
            LinkTableProductAggregate supply,
            LinkTableProductAggregate manual,
            long totalGross,
            long totalDiscount,
            long totalPayment) {
    }

    /**
     * 결제 수단 소스 타입(CASH, CARD, BARCODE 등)별로 건수와 실결제금액을 합산한다.
     *
     * <p>이 단계에서는 grossAmount(정가)와 discountAmount(할인액)는 아직 0이다.
     * 정가·할인액 배분은 {@link #allocateOfflinePaymentAmounts}에서 이루어진다.</p>
     *
     * @param payments 결제 요약 프로젝션 목록 (source_type별 집계)
     * @return 결제 그룹 → 집계값 맵
     */
    Map<String, OfflinePaymentGroupAggregate> aggregateOfflinePayments(List<PaymentSummaryProjection> payments) {
        Map<String, OfflinePaymentGroupAggregate> result = new HashMap<>();

        for (PaymentSummaryProjection payment : payments) {
            String group = groupOfflinePaymentSourceType(payment.getSourceType());
            OfflinePaymentGroupAggregate existing = result.getOrDefault(group, OfflinePaymentGroupAggregate.empty());
            result.put(group, new OfflinePaymentGroupAggregate(
                    existing.paymentCount() + nullToZero(payment.getPaymentCount()),
                    existing.grossAmount(),         // 아직 0 — allocate 단계에서 채워짐
                    existing.discountAmount(),      // 아직 0 — allocate 단계에서 채워짐
                    existing.paymentAmount() + nullToZero(payment.getTotalAmount())));
        }

        return result;
    }

    /**
     * 주문 행(row) 데이터를 이용해 결제 그룹별 grossAmount(정가)와 discountAmount(할인액)를 배분한다.
     *
     * <p>오프라인 주문 한 건은 여러 결제 수단으로 분할 결제될 수 있다.
     * 예: ₩10,000짜리 주문을 현금 ₩6,000 + 카드 ₩4,000으로 결제한 경우,
     * 정가와 할인액도 60% : 40% 비율로 각 그룹에 배분해야 한다.</p>
     *
     * <p>배분 방식:
     * <ol>
     *   <li>비율(ratio) = 해당 결제 수단의 결제금액 / 주문 총금액</li>
     *   <li>배분 정가 = 주문의 정가(list_price) × ratio</li>
     *   <li>배분 할인 = 주문의 할인액(item_discount 우선, 없으면 order_discount) × ratio</li>
     * </ol>
     * </p>
     *
     * @param rows   결제 행별 프로젝션 목록 (주문 1건당 결제 수단 수만큼 행이 있음)
     * @param groups 이미 건수·실결제금액이 채워진 그룹 맵 (이 메서드가 grossAmount·discountAmount를 추가)
     */
    void allocateOfflinePaymentAmounts(
            List<OfflineDailyReportPaymentRowProjection> rows,
            Map<String, OfflinePaymentGroupAggregate> groups) {

        for (OfflineDailyReportPaymentRowProjection row : rows) {
            String groupKey = groupOfflinePaymentSourceType(row.getSourceType());
            OfflinePaymentGroupAggregate existing = groups.getOrDefault(groupKey, OfflinePaymentGroupAggregate.empty());

            long orderTotalAmount = nullToZero(row.getTotalAmount());    // 주문 총금액
            long paymentAmount = nullToZero(row.getPaymentAmount());     // 이 결제 수단이 낸 금액
            // 이 결제 수단이 주문에서 차지하는 비율 (총금액이 0이면 비율 0으로 처리)
            double ratio = orderTotalAmount > 0 ? (double) paymentAmount / orderTotalAmount : 0D;

            long allocatedGross = Math.round(nullToZero(row.getListPrice()) * ratio);

            // 아이템 단위 할인(item_discount)이 있으면 그것을 우선 사용하고,
            // 없으면 주문 헤더 레벨 할인(discount_amount)을 사용한다.
            long orderDiscount = nullToZero(row.getItemDiscountAmount());
            if (orderDiscount == 0L) {
                orderDiscount = nullToZero(row.getDiscountAmount());
            }
            long allocatedDiscount = Math.round(orderDiscount * ratio);

            groups.put(groupKey, new OfflinePaymentGroupAggregate(
                    existing.paymentCount(),
                    existing.grossAmount() + allocatedGross,
                    existing.discountAmount() + allocatedDiscount,
                    existing.paymentAmount()));
        }
    }

    /**
     * 링크 테이블 이름(Sealed / Supply / 그 외)별로 오프라인 판매 수량·금액을 분류·합산한다.
     *
     * <p>링크 테이블은 offline_products.link_table_name 컬럼값으로 결정된다.
     * "Sealed" → 봉입 상품, "Supply" → 용품, 나머지(Manual 등) → 기타</p>
     *
     * <p>결제 금액(paymentAmount)은 gross - discount로 계산하며, 음수가 되지 않도록 0 보정한다.</p>
     *
     * @param rows 링크 테이블별 집계 프로젝션 목록
     * @return 분류별 집계 + 전체 합계
     */
    LinkTableAggregates aggregateLinkTable(List<OfflineDailyReportByLinkTableProjection> rows) {
        LinkTableProductAggregate sealed = LinkTableProductAggregate.empty();
        LinkTableProductAggregate supply = LinkTableProductAggregate.empty();
        LinkTableProductAggregate manual = LinkTableProductAggregate.empty();

        for (OfflineDailyReportByLinkTableProjection row : rows) {
            LinkTableProductAggregate aggregate = new LinkTableProductAggregate(
                    nullToZero(row.getCategoryCount()),
                    nullToZero(row.getTotalQuantity()),
                    nullToZero(row.getGrossAmount()),
                    nullToZero(row.getDiscountAmount()),
                    Math.max(0L, nullToZero(row.getGrossAmount()) - nullToZero(row.getDiscountAmount())));

            switch (row.getLinkTableName()) {
                case "Sealed" -> sealed = mergeLinkTableAggregate(sealed, aggregate);
                case "Supply" -> supply = mergeLinkTableAggregate(supply, aggregate);
                default -> manual = mergeLinkTableAggregate(manual, aggregate);
            }
        }

        long totalGross = sealed.grossAmount() + supply.grossAmount() + manual.grossAmount();
        long totalDiscount = sealed.discountAmount() + supply.discountAmount() + manual.discountAmount();
        long totalPayment = sealed.paymentAmount() + supply.paymentAmount() + manual.paymentAmount();

        return new LinkTableAggregates(sealed, supply, manual, totalGross, totalDiscount, totalPayment);
    }

    /** 세부 카테고리가 없는(간편등록 등) 항목을 표시할 라벨 */
    static final String UNCATEGORIZED_LABEL = "분류 없음";

    /**
     * 오프라인 판매 항목을 링크 테이블(Sealed/Supply/Manual) + 세부 카테고리 단위로
     * 집계한 프로젝션을, 오프라인 매출 세부 DTO로 변환한다.
     *
     * <p>연결된 온라인 상품이 없어 세부 카테고리가 null인 항목(간편등록 등)은
     * "분류 없음"으로 묶어 각 리스트에 포함한다. 매출 비중은 밀봉은 게임 내 전체 금액,
     * 서플라이/수동은 각 리스트 내 전체 금액 대비 %로 계산한다.</p>
     *
     * <p>밀봉 제품은 온라인과 동일하게 게임(product_search_maps.game)별로 분할한다.
     * 서플라이/수동은 게임 구분 없이 세부 카테고리 단위로 합산한다.</p>
     *
     * @param rows 링크 테이블 + 게임 + 카테고리별 집계 프로젝션 목록
     * @return 밀봉(게임별)/서플라이/수동 세부 리스트를 담은 오프라인 매출 세부 DTO
     */
    OfflineSalesSummaryDto aggregateOfflineDetail(List<OfflineDetailByLinkTableProjection> rows) {
        List<OfflineDetailByLinkTableProjection> sealedRows = new ArrayList<>();
        List<OfflineDetailByLinkTableProjection> supplyRows = new ArrayList<>();
        List<OfflineDetailByLinkTableProjection> manualRows = new ArrayList<>();

        for (OfflineDetailByLinkTableProjection row : rows) {
            switch (row.getLinkTableName()) {
                case "Sealed" -> sealedRows.add(row);
                case "Supply" -> supplyRows.add(row);
                default -> manualRows.add(row);
            }
        }

        return OfflineSalesSummaryDto.builder()
                .sealedProductSalesSummaryList(toSealedSummaryList(sealedRows))
                .supplyProductSaleDtoList(toSupplyDetailList(mergeByGroupKey(supplyRows)))
                .manualProductSaleDtoList(toManualDetailList(mergeByGroupKey(manualRows)))
                .build();
    }

    /** 라벨(세부 카테고리)별 합산 결과. 게임 분할을 제거하고 하나로 합칠 때 사용한다. */
    private record MergedRow(String label, long categoryCount, long quantity, long amount) {
    }

    /**
     * 밀봉 제품 집계 결과를 게임별 요약 리스트로 변환한다.
     *
     * <p>게임(game)이 null/빈 값이면 "분류 없음" 게임으로 묶는다.
     * 매출 비중은 게임 내 전체 금액 대비 %로 계산한다.</p>
     */
    private List<SealedProductSalesSummaryDto> toSealedSummaryList(List<OfflineDetailByLinkTableProjection> rows) {
        Map<String, List<OfflineDetailByLinkTableProjection>> byGame = new LinkedHashMap<>();
        for (OfflineDetailByLinkTableProjection r : rows) {
            byGame.computeIfAbsent(categoryLabel(r.getGame()), k -> new ArrayList<>()).add(r);
        }

        List<SealedProductSalesSummaryDto> result = new ArrayList<>();
        for (Map.Entry<String, List<OfflineDetailByLinkTableProjection>> entry : byGame.entrySet()) {
            long gameTotal = sumAmount(entry.getValue());
            List<SealedProductSaleDto> saleDtos = new ArrayList<>();
            for (OfflineDetailByLinkTableProjection r : entry.getValue()) {
                long amount = nullToZero(r.getTotalAmount());
                saleDtos.add(SealedProductSaleDto.builder()
                        .setName(categoryLabel(r.getGroupKey()))
                        .totalSalesCount(nullToZero(r.getCategoryCount()))
                        .totalSalesQuantity(nullToZero(r.getTotalQuantity()))
                        .totalSalesAmount(amount)
                        .totalSalesPercentage(gameTotal > 0 ? (double) amount / gameTotal * 100.0 : null)
                        .build());
            }
            result.add(SealedProductSalesSummaryDto.builder()
                    .game(entry.getKey())
                    .sealedProductSaleDtoList(saleDtos)
                    .build());
        }
        return result;
    }

    /**
     * 게임별로 분할된 행을 세부 카테고리(groupKey) 단위로 다시 합산한다.
     *
     * <p>서플라이/수동은 게임 구분이 필요 없으므로, 쿼리에서 game까지 GROUP BY 된 행들을
     * 라벨 기준으로 재합산해 게임 분할을 제거한다. 금액 내림차순으로 정렬한다.</p>
     */
    private List<MergedRow> mergeByGroupKey(List<OfflineDetailByLinkTableProjection> rows) {
        Map<String, MergedRow> merged = new LinkedHashMap<>();
        for (OfflineDetailByLinkTableProjection r : rows) {
            String label = categoryLabel(r.getGroupKey());
            MergedRow existing = merged.getOrDefault(label, new MergedRow(label, 0L, 0L, 0L));
            merged.put(label, new MergedRow(
                    label,
                    existing.categoryCount() + nullToZero(r.getCategoryCount()),
                    existing.quantity() + nullToZero(r.getTotalQuantity()),
                    existing.amount() + nullToZero(r.getTotalAmount())));
        }
        List<MergedRow> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparingLong((MergedRow r) -> r.amount()).reversed());
        return result;
    }

    private List<SupplyProductSaleDto> toSupplyDetailList(List<MergedRow> rows) {
        long total = rows.stream().mapToLong(r -> r.amount()).sum();
        List<SupplyProductSaleDto> result = new ArrayList<>();
        for (MergedRow r : rows) {
            result.add(SupplyProductSaleDto.builder()
                    .supplyType(r.label())
                    .totalSalesCount(r.categoryCount())
                    .totalSalesQuantity(r.quantity())
                    .totalSalesAmount(r.amount())
                    .totalSalesPercentage(total > 0 ? (double) r.amount() / total * 100.0 : null)
                    .build());
        }
        return result;
    }

    private List<ManualProductSaleDto> toManualDetailList(List<MergedRow> rows) {
        long total = rows.stream().mapToLong(r -> r.amount()).sum();
        List<ManualProductSaleDto> result = new ArrayList<>();
        for (MergedRow r : rows) {
            result.add(ManualProductSaleDto.builder()
                    .manualType(r.label())
                    .totalSalesCount(r.categoryCount())
                    .totalSalesQuantity(r.quantity())
                    .totalSalesAmount(r.amount())
                    .totalSalesPercentage(total > 0 ? (double) r.amount() / total * 100.0 : null)
                    .build());
        }
        return result;
    }

    private long sumAmount(List<OfflineDetailByLinkTableProjection> rows) {
        return rows.stream().mapToLong(r -> nullToZero(r.getTotalAmount())).sum();
    }

    /** 세부 카테고리 값이 null/빈 값이면 "분류 없음" 라벨로 대체한다. */
    private String categoryLabel(String groupKey) {
        return (groupKey == null || groupKey.isBlank()) ? UNCATEGORIZED_LABEL : groupKey;
    }

    /** 두 LinkTableProductAggregate의 모든 필드를 더해 새 인스턴스를 반환한다. */
    private LinkTableProductAggregate mergeLinkTableAggregate(
            LinkTableProductAggregate left,
            LinkTableProductAggregate right) {

        return new LinkTableProductAggregate(
                left.categoryCount() + right.categoryCount(),
                left.quantity() + right.quantity(),
                left.grossAmount() + right.grossAmount(),
                left.discountAmount() + right.discountAmount(),
                left.paymentAmount() + right.paymentAmount());
    }

    /**
     * 결제 소스 타입 문자열을 3개 그룹으로 변환한다.
     *
     * <ul>
     *   <li>CASH → CASH 그룹</li>
     *   <li>CARD, BARCODE → CARD_BARCODE 그룹</li>
     *   <li>그 외 → OTHER 그룹</li>
     * </ul>
     */
    private String groupOfflinePaymentSourceType(String sourceType) {
        if ("CASH".equals(sourceType)) {
            return OFFLINE_GROUP_CASH;
        }
        if ("CARD".equals(sourceType) || "BARCODE".equals(sourceType)) {
            return OFFLINE_GROUP_CARD_BARCODE;
        }
        return OFFLINE_GROUP_OTHER;
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }
}
