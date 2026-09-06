package com.shop.admin.analyze.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.admin.analyze.dto.AdminDailyReportDto;
import com.shop.admin.analyze.dto.AdminDailySalesReportRowDto;
import com.shop.admin.analyze.dto.AdminDailySalesSummarySimpleDto;
import com.shop.admin.analyze.dto.AdminMonthlySalesReportDto;
import com.shop.admin.analyze.dto.AdminMonthlySalesReportRowDto;
import com.shop.admin.analyze.dto.AdminOfflineSalesTotalDto;
import com.shop.admin.analyze.dto.AdminSalesSummarySimpleDto;
import com.shop.admin.analyze.dto.AdminStockSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto;
import com.shop.admin.analyze.dto.AdminWeeklySalesReportRowDto;
import com.shop.common.excel.dto.ExcelFileResult;
import com.shop.common.excel.service.ExcelService;
import com.shop.offline.sales.dto.projection.OfflineDailyAmountProjection;
import com.shop.offline.sales.dto.projection.OfflineDailyReportByLinkTableProjection;
import com.shop.offline.sales.dto.projection.OfflineDailyReportPaymentRowProjection;
import com.shop.offline.sales.dto.projection.OfflineDetailByLinkTableProjection;
import com.shop.offline.sales.dto.projection.OfflineSingleCardReportProjection;
import com.shop.offline.sales.dto.projection.PaymentSummaryProjection;
import com.shop.offline.sales.entity.OfflineSalesInfo;
import com.shop.offline.sales.repository.OfflineSalesInfoRepository;
import com.shop.offline.sales.repository.OfflineSalesItemRepository;
import com.shop.offline.sales.repository.OfflineSalesPaymentRepository;
import com.shop.order.dto.projection.OrderDailyAmountProjection;
import com.shop.order.dto.projection.OrderDailyDeliveryProjection;
import com.shop.order.dto.projection.OrderDailyReportTotalAmountProjection;
import com.shop.order.dto.projection.OrderDailySalesSummaryProjection;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;
import com.shop.product.repository.card.CardProductRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class AdminAnalyzeServiceImpl implements AdminAnalyzeService {

    private static final String OFFLINE_SINGLE_CARDS_TITLE = "Single cards";

    /** 매출 집계 시작일. 목록 페이징의 전체 구간 기준이 된다. */
    private static final LocalDate REPORT_START_DATE = LocalDate.of(2026, 6, 1);

    private final CardProductRepository cardProductRepository;
    private final OrderInfoRepository orderInfoRepository;
    private final OrderProductRepository orderProductRepository;
    private final OfflineSalesInfoRepository offlineSalesInfoRepository;
    private final OfflineSalesItemRepository offlineSalesItemRepository;
    private final OfflineSalesPaymentRepository offlineSalesPaymentRepository;
    private final OnlineSalesAggregator onlineAggregator;
    private final OfflineSalesAggregator offlineAggregator;

    private final ExcelService excelService;
    @Override
    public List<AdminStockSummaryDto> getStockSummary() {
        return cardProductRepository.groupByGameNameAndSumTotalStockCountAndSumTotalStockAmount();
    }

    @Override
    public AdminSalesSummarySimpleDto getCurrentMonthSalesSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDate = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        return toSalesSummary(today.getMonthValue(),
                orderInfoRepository.findSalesTotalsByPeriod(startDate, endDate));
    }

    @Override
    public AdminSalesSummarySimpleDto getTotalSalesSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = REPORT_START_DATE.atStartOfDay();
        LocalDateTime endDate = today.atTime(LocalTime.MAX);

        return toSalesSummary(today.getMonthValue(),
                orderInfoRepository.findSalesTotalsByPeriod(startDate, endDate));
    }

    private AdminSalesSummarySimpleDto toSalesSummary(int orderMonth, OrderDailySalesSummaryProjection totals) {
        return AdminSalesSummarySimpleDto.builder()
                .orderMonth(orderMonth)
                .totalOrderCount(totals.getTotalOrderCount())
                .totalOrderAmount(toLong(totals.getTotalOrderAmount()))
                .totalUsedPointAmount(toLong(totals.getTotalUsedPointAmount()))
                .totalDeliveryFee(toLong(totals.getTotalDeliveryFee()))
                .totalActualPaymentAmount(toLong(totals.getTotalActualPaymentAmount()))
                .build();
    }

    @Override
    public Page<AdminDailySalesSummarySimpleDto> getDailySalesSummary(Pageable pageable) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = REPORT_START_DATE.atStartOfDay();
        LocalDateTime endDate = today.atTime(LocalTime.MAX);
        var dailyPage = orderInfoRepository.findSalesSummaryByPeriod(startDate, endDate, pageable);

        return dailyPage.map(d -> AdminDailySalesSummarySimpleDto.builder()
                .orderDate(d.getOrderDate())
                .totalOrderCount(d.getTotalOrderCount())
                .totalOrderAmount(toLong(d.getTotalOrderAmount()))
                .totalUsedPointAmount(toLong(d.getTotalUsedPointAmount()))
                .totalDeliveryFee(toLong(d.getTotalDeliveryFee()))
                .totalActualPaymentAmount(toLong(d.getTotalActualPaymentAmount()))
                .build());
    }

    @Override
    public Page<AdminDailySalesReportRowDto> getDailySalesReportList(Pageable pageable) {
        LocalDate today = LocalDate.now();
        long total = ChronoUnit.DAYS.between(REPORT_START_DATE, today) + 1;

        long offset = pageable.getOffset();
        if (offset >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        long endIdxExclusive = Math.min(offset + pageable.getPageSize(), total);

        // 인덱스 0 = 오늘(최신), i = 오늘 - i일. 요청 페이지에 해당하는 날짜 구간만 역산한다.
        LocalDate newest = today.minusDays(offset);
        LocalDate oldest = today.minusDays(endIdxExclusive - 1);

        Map<LocalDate, long[]> amountByDate = fetchDailyAmountMap(
                oldest.atStartOfDay(), newest.plusDays(1).atStartOfDay());

        List<AdminDailySalesReportRowDto> pageContent = new ArrayList<>();
        for (LocalDate date = newest; !date.isBefore(oldest); date = date.minusDays(1)) {
            pageContent.add(toDailySalesReportRow(date, amountByDate.getOrDefault(date, new long[3])));
        }
        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    public AdminDailyReportDto getDailySalesReport(String day) {
        LocalDate date = LocalDate.parse(day);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        var paymentHeaders = orderInfoRepository.findDailyReportByPaymentMethod(start, end);
        var paymentLines = orderProductRepository.findDailyReportLineAmountByPaymentMethod(start, end);
        Map<String, OnlineSalesAggregator.PaymentMethodAggregate> onlinePaymentByGroup =
                onlineAggregator.mergeOnlinePaymentAggregates(paymentHeaders, paymentLines);

        OrderDailyDeliveryProjection deliveryProjection = orderInfoRepository.findDailyDelivery(start, end);
        long onlineDeliveryCount = deliveryProjection != null ? nullToZero(deliveryProjection.getDeliveryCount()) : 0L;
        long onlineDeliveryFee   = deliveryProjection != null ? toLong(deliveryProjection.getDeliveryFeeSum()) : 0L;

        var byProductTable = orderProductRepository.findDailyReportByProductTable(start, end);
        OrderDailyReportTotalAmountProjection onlineTotalProjection =
                orderProductRepository.findDailyReportTotalAmount(start, end);
        long onlineProductTotalAmount = onlineTotalProjection != null ? toLong(onlineTotalProjection.getTotalAmount()) : 0L;

        var offlineHeader = offlineSalesInfoRepository.dailySalesSummaryByPeriodStart(
                date, OfflineSalesInfo.ORDER_STATE_COMPLETED).orElse(null);
        long offlineOrderCount = offlineHeader != null ? nullToZero(offlineHeader.getTotalOrderCount()) : 0L;

        List<PaymentSummaryProjection> offlinePayments =
                offlineSalesPaymentRepository.dailyPaymentSummaryByPeriodStarts(
                        List.of(date), OfflineSalesInfo.ORDER_STATE_COMPLETED);
        Map<String, OfflineSalesAggregator.OfflinePaymentGroupAggregate> offlinePaymentByGroup =
                offlineAggregator.aggregateOfflinePayments(offlinePayments);

        List<OfflineDailyReportPaymentRowProjection> paymentRows =
                offlineSalesPaymentRepository.findDailyReportPaymentRows(
                        date, OfflineSalesInfo.ORDER_STATE_COMPLETED);
        offlineAggregator.allocateOfflinePaymentAmounts(paymentRows, offlinePaymentByGroup);

        List<OfflineDailyReportByLinkTableProjection> byLinkTable =
                offlineSalesItemRepository.findDailyReportByLinkTableName(
                        date, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE);
        OfflineSalesAggregator.LinkTableAggregates linkTableAggregates =
                offlineAggregator.aggregateLinkTable(byLinkTable);

        long offlineItemDiscount = nullToZero(
                offlineSalesItemRepository.findDailyReportTotalItemDiscountAmount(
                        date, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE));

        OfflineSingleCardReportProjection singleCardProjection =
                offlineSalesItemRepository.findDailySingleCardReport(
                        date, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE);

        OnlineSalesAggregator.PaymentMethodAggregate storePayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_DIRECT, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.PaymentMethodAggregate cardPayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_ONLINE_CARD, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.PaymentMethodAggregate easyPayPayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_ONLINE_EASY_PAY, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.PaymentMethodAggregate otherPayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_ONLINE_OTHER, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.ProductTableAggregates productAggregates =
                onlineAggregator.aggregateProductTable(byProductTable);

        OfflineSalesAggregator.OfflinePaymentGroupAggregate cashGroup =
                offlinePaymentByGroup.getOrDefault("CASH", OfflineSalesAggregator.OfflinePaymentGroupAggregate.empty());
        OfflineSalesAggregator.OfflinePaymentGroupAggregate cardBarcodeGroup =
                offlinePaymentByGroup.getOrDefault("CARD_BARCODE", OfflineSalesAggregator.OfflinePaymentGroupAggregate.empty());
        OfflineSalesAggregator.OfflinePaymentGroupAggregate otherPaymentGroup =
                offlinePaymentByGroup.getOrDefault("OTHER", OfflineSalesAggregator.OfflinePaymentGroupAggregate.empty());

        long offlinePaymentSum = offlinePayments.stream().mapToLong(p -> nullToZero(p.getTotalAmount())).sum();

        long scGross = singleCardProjection != null ? nullToZero(singleCardProjection.getGrossAmount()) : 0L;
        long scDiscount = singleCardProjection != null ? nullToZero(singleCardProjection.getDiscountAmount()) : 0L;
        long scPayment = Math.max(0L, scGross - scDiscount);
        long offlineTotalGross = linkTableAggregates.totalGross() + scGross;
        long offlineTotalDiscount = linkTableAggregates.totalDiscount() + scDiscount;
        long offlineTotalPayment = linkTableAggregates.totalPayment() + scPayment;

        long onlinePaidOrderCount = cardPayment.orderCount()
                + easyPayPayment.orderCount()
                + otherPayment.orderCount();

        AdminDailyReportDto report = new AdminDailyReportDto();
        // 총계 = 온라인(DIRECT 제외 상품 + 배송료) + 오프라인(싱글카드 포함)
        report.setTotalOrderCount(onlinePaidOrderCount + offlineOrderCount);
        report.setTotalOrderAmount(
                cardPayment.orderAmount() + easyPayPayment.orderAmount()
                        + otherPayment.orderAmount() + onlineDeliveryFee + offlineTotalGross);
        report.setTotalUsedPoint(
                cardPayment.usedPointAmount() + easyPayPayment.usedPointAmount()
                        + otherPayment.usedPointAmount());
        report.setTotalDiscountAmount(offlineItemDiscount + scDiscount);
        report.setTotalPaymentAmount(
                cardPayment.actualPaymentAmount() + easyPayPayment.actualPaymentAmount()
                        + otherPayment.actualPaymentAmount() + offlinePaymentSum);

        report.setTotalOnlineStoreOrderCount(storePayment.orderCount());
        report.setTotalOnlineStoreOrderAmount(storePayment.orderAmount());
        report.setTotalOnlineStoreUsedPoint(storePayment.usedPointAmount());
        report.setTotalOnlineStorePaymentAmount(storePayment.actualPaymentAmount());

        report.setTotalOnlineCardOrderCount(cardPayment.orderCount());
        report.setTotalOnlineCardOrderAmount(cardPayment.orderAmount());
        report.setTotalOnlineCardUsedPoint(cardPayment.usedPointAmount());
        report.setTotalOnlineCardPaymentAmount(cardPayment.actualPaymentAmount());

        report.setTotalOnlineEasyPayOrderCount(easyPayPayment.orderCount());
        report.setTotalOnlineEasyPayOrderAmount(easyPayPayment.orderAmount());
        report.setTotalOnlineEasyPayUsedPoint(easyPayPayment.usedPointAmount());
        report.setTotalOnlineEasyPayPaymentAmount(easyPayPayment.actualPaymentAmount());

        report.setTotalOnlineSingleCardOrderCategoryCount(productAggregates.singleCardCategoryCount());
        report.setTotalOnlineSingleCardOrderQuantity(productAggregates.singleCardQuantity());
        report.setTotalOnlineSingleCardOrderAmount(productAggregates.singleCardAmount());

        report.setTotalOnlineSealedProductOrderCategoryCount(productAggregates.sealedCategoryCount());
        report.setTotalOnlineSealedProductOrderQuantity(productAggregates.sealedQuantity());
        report.setTotalOnlineSealedProductOrderAmount(productAggregates.sealedAmount());

        report.setTotalOnlineSupplyOrderCount(productAggregates.supplyCategoryCount());
        report.setTotalOnlineSupplyOrderAmount(productAggregates.supplyAmount());
        report.setTotalOnlineSupplyOrderQuantity(productAggregates.supplyQuantity());

        report.setTotalOnlineOtherProductOrderCategoryCount(productAggregates.otherCategoryCount());
        report.setTotalOnlineOtherProductOrderQuantity(productAggregates.otherQuantity());
        report.setTotalOnlineOtherProductAmount(productAggregates.otherAmount());
        report.setTotalOnlineDeliveryCount(onlineDeliveryCount);
        report.setTotalOnlineDeliveryFee(onlineDeliveryFee);

        report.setTotalOnlineTotalAmount(onlineProductTotalAmount + onlineDeliveryFee);

        report.setTotalOfflineCashCount(cashGroup.paymentCount());
        report.setTotalOfflineCashAmount(cashGroup.grossAmount());
        report.setTotalOfflineCashDiscountAmount(cashGroup.discountAmount());
        report.setTotalOfflineCashPaymentAmount(cashGroup.paymentAmount());

        report.setTotalOfflineCardOrderCount(cardBarcodeGroup.paymentCount());
        report.setTotalOfflineCardOrderAmount(cardBarcodeGroup.grossAmount());
        report.setTotalOfflineCardDiscountAmount(cardBarcodeGroup.discountAmount());
        report.setTotalOfflineCardPaymentAmount(cardBarcodeGroup.paymentAmount());

        report.setTotalOfflineOtherPaymentCount(otherPaymentGroup.paymentCount());
        report.setTotalOfflineOtherPaymentAmount(otherPaymentGroup.grossAmount());
        report.setTotalOfflineOtherPaymentDiscountAmount(otherPaymentGroup.discountAmount());
        report.setTotalOfflineOtherPaymentPaymentAmount(otherPaymentGroup.paymentAmount());

        report.setTotalOfflineSingleCardOrderCategoryCount(singleCardProjection != null ? nullToZero(singleCardProjection.getCategoryCount()) : 0L);
        report.setTotalOfflineSingleCardOrderQuantity(singleCardProjection != null ? nullToZero(singleCardProjection.getTotalQuantity()) : 0L);
        report.setTotalOfflineSingleCardOrderAmount(scGross);
        report.setTotalOfflineSingleCardDiscountAmount(scDiscount);
        report.setTotalOfflineSingleCardPaymentAmount(scPayment);

        report.setTotalOfflineSealedProductOrderCategoryCount(linkTableAggregates.sealed().categoryCount());
        report.setTotalOfflineSealedProductOrderQuantity(linkTableAggregates.sealed().quantity());
        report.setTotalOfflineSealedProductOrderAmount(linkTableAggregates.sealed().grossAmount());
        report.setTotalOfflineSealedProductDiscountAmount(linkTableAggregates.sealed().discountAmount());
        report.setTotalOfflineSealedProductPaymentAmount(linkTableAggregates.sealed().paymentAmount());

        report.setTotalOfflineSupplyOrderCount(linkTableAggregates.supply().categoryCount());
        report.setTotalOfflineSupplyOrderAmount(linkTableAggregates.supply().grossAmount());
        report.setTotalOfflineSupplyOrderQuantity(linkTableAggregates.supply().quantity());
        report.setTotalOfflineSupplyDiscountAmount(linkTableAggregates.supply().discountAmount());
        report.setTotalOfflineSupplyPaymentAmount(linkTableAggregates.supply().paymentAmount());

        report.setTotalOfflineOtherProductCount(linkTableAggregates.manual().categoryCount());
        report.setTotalOfflineOtherProductAmount(linkTableAggregates.manual().grossAmount());
        report.setTotalOfflineOtherProductQuantity(linkTableAggregates.manual().quantity());
        report.setTotalOfflineOtherProductDiscountAmount(linkTableAggregates.manual().discountAmount());
        report.setTotalOfflineOtherProductPaymentAmount(linkTableAggregates.manual().paymentAmount());

        report.setTotalOfflineTotalAmount(offlineTotalGross);
        report.setTotalOfflineTotalDiscountAmount(offlineTotalDiscount);
        report.setTotalOfflineTotalPaymentAmount(offlineTotalPayment);

        return report;
    }

    @Override
    public Page<AdminWeeklySalesReportRowDto> getWeeklySalesReportList(Pageable pageable) {
        LocalDate currentMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate firstMonday = REPORT_START_DATE.with(DayOfWeek.MONDAY);
        long total = ChronoUnit.WEEKS.between(firstMonday, currentMonday) + 1;

        long offset = pageable.getOffset();
        if (offset >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        long endIdxExclusive = Math.min(offset + pageable.getPageSize(), total);

        LocalDate newestMonday = currentMonday.minusWeeks(offset);
        LocalDate oldestMonday = currentMonday.minusWeeks(endIdxExclusive - 1);

        Map<LocalDate, long[]> amountByWeek = aggregateDailyAmountsByWeek(fetchDailyAmountMap(
                oldestMonday.atStartOfDay(), newestMonday.plusWeeks(1).atStartOfDay()));

        List<AdminWeeklySalesReportRowDto> pageContent = new ArrayList<>();
        for (LocalDate monday = newestMonday; !monday.isBefore(oldestMonday); monday = monday.minusWeeks(1)) {
            long[] amounts = amountByWeek.getOrDefault(monday, new long[3]);
            long online = amounts[0];
            long offline = amounts[1];
            long onlineForTotal = amounts[2];
            pageContent.add(AdminWeeklySalesReportRowDto.builder()
                    .monday(monday)
                    .onlineAmount(online)
                    .offlineAmount(offline)
                    .totalAmount(onlineForTotal + offline)
                    .build());
        }
        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    public AdminWeeklyReportDto getWeeklySalesReport(String monday) {
        LocalDate mondayDate = LocalDate.parse(monday);
        LocalDateTime start = mondayDate.atStartOfDay();
        LocalDateTime end = mondayDate.plusWeeks(1).atStartOfDay();
        return buildInto(new AdminWeeklyReportDto(), start, end);
    }

    @Override
    public Page<AdminMonthlySalesReportRowDto> getMonthlySalesReportList(Pageable pageable) {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate firstMonth = REPORT_START_DATE.withDayOfMonth(1);
        long total = ChronoUnit.MONTHS.between(firstMonth, currentMonth) + 1;

        long offset = pageable.getOffset();
        if (offset >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        long endIdxExclusive = Math.min(offset + pageable.getPageSize(), total);

        LocalDate newestMonth = currentMonth.minusMonths(offset);
        LocalDate oldestMonth = currentMonth.minusMonths(endIdxExclusive - 1);

        Map<LocalDate, long[]> amountByMonth = aggregateDailyAmountsByMonth(fetchDailyAmountMap(
                oldestMonth.atStartOfDay(), newestMonth.plusMonths(1).atStartOfDay()));

        List<AdminMonthlySalesReportRowDto> pageContent = new ArrayList<>();
        for (LocalDate month = newestMonth; !month.isBefore(oldestMonth); month = month.minusMonths(1)) {
            long[] amounts = amountByMonth.getOrDefault(month, new long[3]);
            long online = amounts[0];
            long offline = amounts[1];
            long onlineForTotal = amounts[2];
            pageContent.add(AdminMonthlySalesReportRowDto.builder()
                    .month(month)
                    .onlineAmount(online)
                    .offlineAmount(offline)
                    .totalAmount(onlineForTotal + offline)
                    .build());
        }
        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    public AdminMonthlySalesReportDto getMonthlySalesReport(String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();
        return buildInto(new AdminMonthlySalesReportDto(), start, end);
    }

    private <T extends AdminWeeklyReportDto> T buildInto(T report, LocalDateTime start, LocalDateTime end) {
        var paymentHeaders = orderInfoRepository.findDailyReportByPaymentMethod(start, end);
        var paymentLines = orderProductRepository.findDailyReportLineAmountByPaymentMethod(start, end);
        Map<String, OnlineSalesAggregator.PaymentMethodAggregate> onlinePaymentByGroup =
                onlineAggregator.mergeOnlinePaymentAggregates(paymentHeaders, paymentLines);

        var deliveryProjection = orderInfoRepository.findDailyDelivery(start, end);
        long onlineDeliveryCount = deliveryProjection != null ? nullToZero(deliveryProjection.getDeliveryCount()) : 0L;
        long onlineDeliveryFee = deliveryProjection != null ? toLong(deliveryProjection.getDeliveryFeeSum()) : 0L;

        var byProductTable = orderProductRepository.findDailyReportByProductTable(start, end);
        var onlineTotalProjection = orderProductRepository.findDailyReportTotalAmount(start, end);
        long onlineProductTotalAmount = onlineTotalProjection != null ? toLong(onlineTotalProjection.getTotalAmount()) : 0L;

        var offlineHeader = offlineSalesInfoRepository.findSalesSummaryByPeriod(
                start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED).orElse(null);
        long offlineOrderCount = offlineHeader != null ? nullToZero(offlineHeader.getTotalOrderCount()) : 0L;

        List<PaymentSummaryProjection> offlinePayments =
                offlineSalesPaymentRepository.findPaymentSummaryByPeriod(
                        start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED);
        Map<String, OfflineSalesAggregator.OfflinePaymentGroupAggregate> offlinePaymentByGroup =
                offlineAggregator.aggregateOfflinePayments(offlinePayments);

        List<OfflineDailyReportPaymentRowProjection> paymentRows =
                offlineSalesPaymentRepository.findReportPaymentRowsByPeriod(
                        start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED);
        offlineAggregator.allocateOfflinePaymentAmounts(paymentRows, offlinePaymentByGroup);

        List<OfflineDailyReportByLinkTableProjection> byLinkTable =
                offlineSalesItemRepository.findReportByLinkTableNameByPeriod(
                        start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE);
        OfflineSalesAggregator.LinkTableAggregates linkTableAggregates =
                offlineAggregator.aggregateLinkTable(byLinkTable);

        long offlineItemDiscount = nullToZero(
                offlineSalesItemRepository.findReportTotalItemDiscountAmountByPeriod(
                        start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE));

        OfflineSingleCardReportProjection singleCardProjection =
                offlineSalesItemRepository.findSingleCardReportByPeriod(
                        start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE);

        long offlinePaymentSum = offlinePayments.stream().mapToLong(p -> nullToZero(p.getTotalAmount())).sum();

        OnlineSalesAggregator.PaymentMethodAggregate storePayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_DIRECT, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.PaymentMethodAggregate cardPayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_ONLINE_CARD, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.PaymentMethodAggregate easyPayPayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_ONLINE_EASY_PAY, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.PaymentMethodAggregate otherPayment =
                onlinePaymentByGroup.getOrDefault(OnlineSalesAggregator.PAYMENT_GROUP_ONLINE_OTHER, OnlineSalesAggregator.PaymentMethodAggregate.empty());
        OnlineSalesAggregator.ProductTableAggregates productAggregates =
                onlineAggregator.aggregateProductTable(byProductTable);

        OfflineSalesAggregator.OfflinePaymentGroupAggregate cashGroup =
                offlinePaymentByGroup.getOrDefault("CASH", OfflineSalesAggregator.OfflinePaymentGroupAggregate.empty());
        OfflineSalesAggregator.OfflinePaymentGroupAggregate cardBarcodeGroup =
                offlinePaymentByGroup.getOrDefault("CARD_BARCODE", OfflineSalesAggregator.OfflinePaymentGroupAggregate.empty());
        OfflineSalesAggregator.OfflinePaymentGroupAggregate otherPaymentGroup =
                offlinePaymentByGroup.getOrDefault("OTHER", OfflineSalesAggregator.OfflinePaymentGroupAggregate.empty());

        long scGross = singleCardProjection != null ? nullToZero(singleCardProjection.getGrossAmount()) : 0L;
        long scDiscount = singleCardProjection != null ? nullToZero(singleCardProjection.getDiscountAmount()) : 0L;
        long scPayment = Math.max(0L, scGross - scDiscount);
        long offlineTotalGross = linkTableAggregates.totalGross() + scGross;
        long offlineTotalDiscount = linkTableAggregates.totalDiscount() + scDiscount;
        long offlineTotalPayment = linkTableAggregates.totalPayment() + scPayment;

        var cardDetailRows = orderProductRepository.findOnlineCardProductDetail(start, end);
        var sealedDetailRows = orderProductRepository.findOnlineSealedProductDetail(start, end);
        var supplyDetailRows = orderProductRepository.findOnlineSupplyDetail(start, end);
        var manualDetailRows = orderProductRepository.findOnlineManualDetail(start, end);

        List<OfflineDetailByLinkTableProjection> offlineDetailRows =
                offlineSalesItemRepository.findOfflineDetailByLinkTableByPeriod(
                        start, end, OfflineSalesInfo.ORDER_STATE_COMPLETED, OFFLINE_SINGLE_CARDS_TITLE);

        long onlinePaidOrderCount = cardPayment.orderCount()
                + easyPayPayment.orderCount()
                + otherPayment.orderCount();

        // 총계 = 온라인(DIRECT 제외 상품 + 배송료) + 오프라인(싱글카드 포함)
        report.setTotalOrderCount(onlinePaidOrderCount + offlineOrderCount);
        report.setTotalOrderAmount(
                cardPayment.orderAmount() + easyPayPayment.orderAmount()
                        + otherPayment.orderAmount() + onlineDeliveryFee + offlineTotalGross);
        report.setTotalUsedPoint(
                cardPayment.usedPointAmount() + easyPayPayment.usedPointAmount()
                        + otherPayment.usedPointAmount());
        report.setTotalDiscountAmount(offlineItemDiscount + scDiscount);
        report.setTotalPaymentAmount(
                cardPayment.actualPaymentAmount() + easyPayPayment.actualPaymentAmount()
                        + otherPayment.actualPaymentAmount() + offlinePaymentSum);

        report.setTotalOnlineStoreOrderCount(storePayment.orderCount());
        report.setTotalOnlineStoreOrderAmount(storePayment.orderAmount());
        report.setTotalOnlineStoreUsedPoint(storePayment.usedPointAmount());
        report.setTotalOnlineStorePaymentAmount(storePayment.actualPaymentAmount());

        report.setTotalOnlineCardOrderCount(cardPayment.orderCount());
        report.setTotalOnlineCardOrderAmount(cardPayment.orderAmount());
        report.setTotalOnlineCardUsedPoint(cardPayment.usedPointAmount());
        report.setTotalOnlineCardPaymentAmount(cardPayment.actualPaymentAmount());

        report.setTotalOnlineEasyPayOrderCount(easyPayPayment.orderCount());
        report.setTotalOnlineEasyPayOrderAmount(easyPayPayment.orderAmount());
        report.setTotalOnlineEasyPayUsedPoint(easyPayPayment.usedPointAmount());
        report.setTotalOnlineEasyPayPaymentAmount(easyPayPayment.actualPaymentAmount());

        report.setTotalOnlineSingleCardOrderCategoryCount(productAggregates.singleCardCategoryCount());
        report.setTotalOnlineSingleCardOrderQuantity(productAggregates.singleCardQuantity());
        report.setTotalOnlineSingleCardOrderAmount(productAggregates.singleCardAmount());

        report.setTotalOnlineSealedProductOrderCategoryCount(productAggregates.sealedCategoryCount());
        report.setTotalOnlineSealedProductOrderQuantity(productAggregates.sealedQuantity());
        report.setTotalOnlineSealedProductOrderAmount(productAggregates.sealedAmount());

        report.setTotalOnlineSupplyOrderCount(productAggregates.supplyCategoryCount());
        report.setTotalOnlineSupplyOrderAmount(productAggregates.supplyAmount());
        report.setTotalOnlineSupplyOrderQuantity(productAggregates.supplyQuantity());

        report.setTotalOnlineOtherProductOrderCategoryCount(productAggregates.otherCategoryCount());
        report.setTotalOnlineOtherProductOrderQuantity(productAggregates.otherQuantity());
        report.setTotalOnlineOtherProductAmount(productAggregates.otherAmount());
        report.setTotalOnlineDeliveryCount(onlineDeliveryCount);
        report.setTotalOnlineDeliveryFee(onlineDeliveryFee);

        report.setTotalOnlineTotalAmount(onlineProductTotalAmount + onlineDeliveryFee);

        report.setTotalOfflineCashCount(cashGroup.paymentCount());
        report.setTotalOfflineCashAmount(cashGroup.grossAmount());
        report.setTotalOfflineCashDiscountAmount(cashGroup.discountAmount());
        report.setTotalOfflineCashPaymentAmount(cashGroup.paymentAmount());

        report.setTotalOfflineCardOrderCount(cardBarcodeGroup.paymentCount());
        report.setTotalOfflineCardOrderAmount(cardBarcodeGroup.grossAmount());
        report.setTotalOfflineCardDiscountAmount(cardBarcodeGroup.discountAmount());
        report.setTotalOfflineCardPaymentAmount(cardBarcodeGroup.paymentAmount());

        report.setTotalOfflineOtherPaymentCount(otherPaymentGroup.paymentCount());
        report.setTotalOfflineOtherPaymentAmount(otherPaymentGroup.grossAmount());
        report.setTotalOfflineOtherPaymentDiscountAmount(otherPaymentGroup.discountAmount());
        report.setTotalOfflineOtherPaymentPaymentAmount(otherPaymentGroup.paymentAmount());

        report.setTotalOfflineSingleCardOrderCategoryCount(singleCardProjection != null ? nullToZero(singleCardProjection.getCategoryCount()) : 0L);
        report.setTotalOfflineSingleCardOrderQuantity(singleCardProjection != null ? nullToZero(singleCardProjection.getTotalQuantity()) : 0L);
        report.setTotalOfflineSingleCardOrderAmount(scGross);
        report.setTotalOfflineSingleCardDiscountAmount(scDiscount);
        report.setTotalOfflineSingleCardPaymentAmount(scPayment);

        report.setTotalOfflineSealedProductOrderCategoryCount(linkTableAggregates.sealed().categoryCount());
        report.setTotalOfflineSealedProductOrderQuantity(linkTableAggregates.sealed().quantity());
        report.setTotalOfflineSealedProductOrderAmount(linkTableAggregates.sealed().grossAmount());
        report.setTotalOfflineSealedProductDiscountAmount(linkTableAggregates.sealed().discountAmount());
        report.setTotalOfflineSealedProductPaymentAmount(linkTableAggregates.sealed().paymentAmount());

        report.setTotalOfflineSupplyOrderCount(linkTableAggregates.supply().categoryCount());
        report.setTotalOfflineSupplyOrderAmount(linkTableAggregates.supply().grossAmount());
        report.setTotalOfflineSupplyOrderQuantity(linkTableAggregates.supply().quantity());
        report.setTotalOfflineSupplyDiscountAmount(linkTableAggregates.supply().discountAmount());
        report.setTotalOfflineSupplyPaymentAmount(linkTableAggregates.supply().paymentAmount());

        report.setTotalOfflineOtherProductCount(linkTableAggregates.manual().categoryCount());
        report.setTotalOfflineOtherProductAmount(linkTableAggregates.manual().grossAmount());
        report.setTotalOfflineOtherProductQuantity(linkTableAggregates.manual().quantity());
        report.setTotalOfflineOtherProductDiscountAmount(linkTableAggregates.manual().discountAmount());
        report.setTotalOfflineOtherProductPaymentAmount(linkTableAggregates.manual().paymentAmount());

        report.setTotalOfflineTotalAmount(offlineTotalGross);
        report.setTotalOfflineTotalDiscountAmount(offlineTotalDiscount);
        report.setTotalOfflineTotalPaymentAmount(offlineTotalPayment);

        report.setOnlineSalesSummaryDto(AdminWeeklyReportDto.OnlineSalesSummaryDto.builder()
                .singleCardSalesSummaryList(onlineAggregator.aggregateSingleCardDetails(cardDetailRows))
                .sealedProductSalesSummaryList(onlineAggregator.aggregateSealedProductDetails(sealedDetailRows))
                .supplyProductSalesSummaryDto(onlineAggregator.aggregateSupplyDetails(supplyDetailRows))
                .manualProductSalesSummaryDto(onlineAggregator.aggregateManualDetails(manualDetailRows))
                .build());

        report.setOfflineSalesSummaryDto(offlineAggregator.aggregateOfflineDetail(offlineDetailRows));
        report.setDailySalesReportRowDtoList(totalAmountByPeriod(start, end));

        return report;
    }

    /**
     * 기간 내 일별 온라인·오프라인 매출을 합산한 행 목록을 만든다.
     *
     * <p>온라인 금액({@code onlineAmount})은 DIRECT 포함 + 배송료.
     * 오프라인은 싱글카드 포함. 총 매출({@code totalAmount})만 DIRECT 상품을 제외한다.</p>
     */
    private List<AdminDailySalesReportRowDto> totalAmountByPeriod(
            LocalDateTime startDate, LocalDateTime endDate) {
        Map<LocalDate, long[]> amountByDate = fetchDailyAmountMap(startDate, endDate);
        LocalDate start = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        return start.datesUntil(end)
                .map(date -> toDailySalesReportRow(date, amountByDate.getOrDefault(date, new long[3])))
                .collect(Collectors.toList());
    }

    /**
     * 기간 내 일별 온라인·오프라인 매출을 합산한다. 주간·월간 상세 보고와 목록 API에서 공유한다.
     *
     * <p>배열 인덱스:
     * [0] 온라인(상품 DIRECT 포함 + 배송료),
     * [1] 오프라인(싱글카드 포함),
     * [2] 온라인(상품 DIRECT 제외 + 배송료, 총매출용).</p>
     */
    private Map<LocalDate, long[]> fetchDailyAmountMap(LocalDateTime startDate, LocalDateTime endDate) {
        Map<LocalDate, long[]> amountByDate = new TreeMap<>();

        for (OrderDailyAmountProjection online : orderProductRepository.findDailyProductAmountByPeriod(startDate, endDate)) {
            if (online.getOrderDate() == null) {
                continue;
            }
            amountByDate.computeIfAbsent(online.getOrderDate(), d -> new long[3])[0] += toLong(online.getTotalAmount());
        }

        for (OrderDailyAmountProjection online : orderProductRepository.findDailyProductAmountByPeriodExcludingDirect(startDate, endDate)) {
            if (online.getOrderDate() == null) {
                continue;
            }
            amountByDate.computeIfAbsent(online.getOrderDate(), d -> new long[3])[2] += toLong(online.getTotalAmount());
        }

        for (OrderDailyAmountProjection delivery : orderInfoRepository.findDailyDeliveryFeeByPeriod(startDate, endDate)) {
            if (delivery.getOrderDate() == null) {
                continue;
            }
            long fee = toLong(delivery.getTotalAmount());
            long[] amounts = amountByDate.computeIfAbsent(delivery.getOrderDate(), d -> new long[3]);
            amounts[0] += fee;
            amounts[2] += fee;
        }

        for (OfflineDailyAmountProjection offline : offlineSalesItemRepository.findDailyItemAmountByPeriod(
                startDate, endDate, OfflineSalesInfo.ORDER_STATE_COMPLETED)) {
            if (offline.getOrderDate() == null) {
                continue;
            }
            amountByDate.computeIfAbsent(offline.getOrderDate(), d -> new long[3])[1] += nullToZero(offline.getTotalAmount());
        }

        return amountByDate;
    }

    private Map<LocalDate, long[]> aggregateDailyAmountsByWeek(Map<LocalDate, long[]> dailyAmounts) {
        Map<LocalDate, long[]> amountByWeek = new TreeMap<>();
        for (Map.Entry<LocalDate, long[]> entry : dailyAmounts.entrySet()) {
            LocalDate monday = entry.getKey().with(DayOfWeek.MONDAY);
            long[] weekAmounts = amountByWeek.computeIfAbsent(monday, d -> new long[3]);
            weekAmounts[0] += entry.getValue()[0];
            weekAmounts[1] += entry.getValue()[1];
            weekAmounts[2] += entry.getValue()[2];
        }
        return amountByWeek;
    }

    private Map<LocalDate, long[]> aggregateDailyAmountsByMonth(Map<LocalDate, long[]> dailyAmounts) {
        Map<LocalDate, long[]> amountByMonth = new TreeMap<>();
        for (Map.Entry<LocalDate, long[]> entry : dailyAmounts.entrySet()) {
            LocalDate firstOfMonth = entry.getKey().withDayOfMonth(1);
            long[] monthAmounts = amountByMonth.computeIfAbsent(firstOfMonth, d -> new long[3]);
            monthAmounts[0] += entry.getValue()[0];
            monthAmounts[1] += entry.getValue()[1];
            monthAmounts[2] += entry.getValue()[2];
        }
        return amountByMonth;
    }

    private AdminDailySalesReportRowDto toDailySalesReportRow(LocalDate date, long[] amounts) {
        long online = amounts[0];
        long offline = amounts[1];
        long onlineForTotal = amounts[2];
        return AdminDailySalesReportRowDto.builder()
                .orderDate(date)
                .onlineAmount(online)
                .offlineAmount(offline)
                .totalAmount(onlineForTotal + offline)
                .build();
    }

    private long toLong(BigDecimal value) {
        return value != null ? value.longValue() : 0L;
    }

    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    @Override
    public Page<AdminOfflineSalesTotalDto> getOfflineSalesTotal(Pageable pageable, String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        return offlineSalesItemRepository.findOfflineSalesTotal(start, end, pageable)
                .map(p -> new AdminOfflineSalesTotalDto(
                        p.getCategory(),
                        p.getItem(),
                        nullToZero(p.getQuantity()),
                        nullToZero(p.getAmount())));
    }

    @Override
    public ExcelFileResult downloadOfflineSalesTotal(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);


        List<AdminOfflineSalesTotalDto> offlineSalesTotalList = offlineSalesItemRepository.findOfflineSalesTotalList(start, end)
                .stream()
                .map(p -> new AdminOfflineSalesTotalDto(
                        p.getCategory(),
                        p.getItem(),
                        nullToZero(p.getQuantity()),
                        nullToZero(p.getAmount())))
                .collect(Collectors.toList());
        return excelService.generateOfflineSalesTotalExcel(offlineSalesTotalList, startDate, endDate);
    }
}
