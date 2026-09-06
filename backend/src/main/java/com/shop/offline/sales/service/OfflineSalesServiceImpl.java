package com.shop.offline.sales.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.offline.product.service.OfflineShippingQuantityAdjuster;
import com.shop.offline.sales.dto.OfflineSalesAppliedDiscountDto;
import com.shop.offline.sales.dto.OfflineSalesInfoDto;
import com.shop.offline.sales.dto.OfflineSalesItemDto;
import com.shop.offline.sales.dto.OfflineSalesPaymentDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryItemDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryPaymentDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryPeriod;
import com.shop.offline.sales.dto.projection.PaymentSummaryProjection;
import com.shop.offline.sales.dto.projection.SalesSummaryItemProjection;
import com.shop.offline.sales.dto.projection.SalesSummaryProjection;
import com.shop.offline.sales.dto.raw.OfflineSalesRawDto;
import com.shop.offline.sales.dto.raw.OfflineSalesResponse;
import com.shop.offline.sales.entity.OfflineSalesInfo;
import com.shop.offline.sales.entity.OfflineSalesItem;
import com.shop.offline.sales.entity.OfflineSalesItemDiscount;
import com.shop.offline.sales.entity.OfflineSalesPayment;
import com.shop.offline.sales.entity.OfflineSalesSyncState;
import com.shop.offline.sales.repository.OfflineSalesInfoRepository;
import com.shop.offline.sales.repository.OfflineSalesItemRepository;
import com.shop.offline.sales.repository.OfflineSalesPaymentRepository;
import com.shop.offline.sales.repository.OfflineSalesSyncStateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfflineSalesServiceImpl implements OfflineSalesService {

    private static final String TOSS_OPEN_API_URL = "https://open-api.tossplace.com/api-public/openapi/v1/merchants/{merchantId}/";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int PAGE_SIZE = 500;
    private static final int WEBCLIENT_MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;

    private final WebClient webClient = WebClient.builder()
            .codecs(codecConfigurer -> codecConfigurer.defaultCodecs()
                    .maxInMemorySize(WEBCLIENT_MAX_IN_MEMORY_BYTES))
            .build();
    private final ObjectMapper objectMapper;
    private final OfflineSalesSyncStateRepository offlineSalesSyncStateRepository;
    private final OfflineSalesInfoRepository offlineSalesInfoRepository;
    private final OfflineSalesItemRepository offlineSalesItemRepository;
    private final OfflineSalesPaymentRepository offlineSalesPaymentRepository;
    private final OfflineShippingQuantityAdjuster offlineShippingQuantityAdjuster;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final AtomicBoolean downloadRunning = new AtomicBoolean(false);

    @Value("${app.toss-place.access-key}")
    private String accessKey;
    @Value("${app.toss-place.secret-key}")
    private String secretKey;
    @Value("${app.toss-place.merchant-id}")
    private String merchantId;

    @Override
    public boolean isDownloadRunning() {
        return downloadRunning.get();
    }

    @Override
    public boolean startDownloadOfflineSalesbyPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (!downloadRunning.compareAndSet(false, true)) {
            log.warn("Offline sales download skipped — already running. period={} ~ {}", startDate, endDate);
            return false;
        }
        try {
            eventPublisher.publishEvent(new OfflineSalesDownloadRequested(startDate, endDate));
            return true;
        } catch (RuntimeException e) {
            downloadRunning.set(false);
            throw e;
        }
    }

    @Async
    @EventListener
    public void onOfflineSalesDownloadRequested(OfflineSalesDownloadRequested event) {
        try {
            executeDownload(event.startDate(), event.endDate());
        } catch (Exception e) {
            persistSyncStateError(e);
            log.error("Offline sales async download failed. period={} ~ {}",
                    event.startDate(), event.endDate(), e);
        } finally {
            downloadRunning.set(false);
        }
    }

    @Override
    public boolean downloadOfflineSalesbyPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (!downloadRunning.compareAndSet(false, true)) {
            log.warn("Offline sales download skipped — already running. period={} ~ {}", startDate, endDate);
            return false;
        }
        try {
            executeDownload(startDate, endDate);
            return true;
        } catch (RuntimeException e) {
            persistSyncStateError(e);
            throw e;
        } finally {
            downloadRunning.set(false);
        }
    }

    private void executeDownload(LocalDateTime startDate, LocalDateTime endDate) {
        String start = startDate.toLocalDate().toString();
        String end = endDate.toLocalDate().toString();
        int page = 1;
        while (fetchAndSaveOfflineSales(page, start, end)) {
            page++;
        }
        persistSyncStateSuccess();
    }

    /**
     * 수동 다운로드 비동기 시작용 내부 이벤트.
     */
    record OfflineSalesDownloadRequested(LocalDateTime startDate, LocalDateTime endDate) {
    }

    private boolean fetchAndSaveOfflineSales(int page, String start, String end) {
        String url = buildTossApiUrl(
                "order/orders?start=" + start + "&end=" + end
                        + "&page=" + page + "&size=" + PAGE_SIZE + "&sortOrder=DESC");
        String response = webClient.get()
                .uri(url)
                .header("X-access-key", accessKey)
                .header("X-secret-key", secretKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        // HTTP 호출과 분리해 페이지 단위로만 트랜잭션을 연다 (장시간 커넥션 점유·타임아웃 방지)
        Boolean hasMore = transactionTemplate.execute(status -> saveOfflineSales(response));
        return Boolean.TRUE.equals(hasMore);
    }

    private String buildTossApiUrl(String path) {
        return TOSS_OPEN_API_URL.replace("{merchantId}", merchantId) + path;
    }

    @Transactional
    private boolean saveOfflineSales(String response) {
        try {
            OfflineSalesResponse salesResponse = objectMapper.readValue(response, OfflineSalesResponse.class);
            if (!"SUCCESS".equals(salesResponse.getResultType())) {
                log.warn("오프라인 판매 API 응답 실패: resultType={}", salesResponse.getResultType());
                return false;
            }
            List<OfflineSalesRawDto> salesRawDtos = salesResponse.getSuccess() != null
                    ? salesResponse.getSuccess()
                    : List.of();
            if (salesRawDtos.isEmpty()) {
                return false;
            }
            for (OfflineSalesRawDto dto : salesRawDtos) {
                if (dto.getId() == null) {
                    log.warn("오프라인 판매 필수 필드 누락: orderId=null");
                    continue;
                }
                Map<String, Integer> beforeContribution = captureExistingContribution(dto.getId());
                OfflineSalesInfo info = upsertOfflineSalesInfo(dto);
                syncPayments(info, dto);
                saveLineItems(info, dto);
                Map<String, Integer> afterContribution = captureDtoContribution(dto);
                offlineShippingQuantityAdjuster.applyTitleQuantityDeltas(
                        OfflineShippingQuantityAdjuster.diffContributions(
                                beforeContribution, afterContribution));
            }
            return salesRawDtos.size() >= PAGE_SIZE;
        } catch (JsonProcessingException e) {
            log.error("오프라인 판매 응답 파싱 실패: {}", e.getMessage());
            return false;
        }
    }

    private void saveLineItems(OfflineSalesInfo info, OfflineSalesRawDto dto) {
        if (dto.getLineItems() == null) {
            return;
        }
        List<OfflineSalesRawDto.LineItemRawDto> lineItems = dto.getLineItems();
        for (int index = 0; index < lineItems.size(); index++) {
            OfflineSalesRawDto.LineItemRawDto itemDto = lineItems.get(index);
            String lineItemId = buildLineItemKey(dto.getId(), index);
            upsertOfflineSalesItem(info, itemDto, lineItemId);
        }
    }

    /**
     * upsert 전 DB 상태 기준 COMPLETED 기여 수량 (title → quantity).
     */
    private Map<String, Integer> captureExistingContribution(String orderId) {
        List<OfflineSalesInfo> existingList = offlineSalesInfoRepository.findAllByOrderId(orderId);
        if (existingList.isEmpty()) {
            return Map.of();
        }
        OfflineSalesInfo existing = existingList.get(0);
        if (!OfflineSalesInfo.ORDER_STATE_COMPLETED.equals(existing.getOrderState())) {
            return Map.of();
        }
        List<OfflineSalesItem> items = offlineSalesItemRepository.findAllByOfflineSalesInfoId(existing.getId());
        return aggregateByTitle(items);
    }

    /**
     * upsert 후 DTO 기준 COMPLETED 기여 수량 (title → quantity).
     */
    private Map<String, Integer> captureDtoContribution(OfflineSalesRawDto dto) {
        if (!OfflineSalesInfo.ORDER_STATE_COMPLETED.equals(dto.getOrderState())) {
            return Map.of();
        }
        if (dto.getLineItems() == null || dto.getLineItems().isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> contribution = new HashMap<>();
        for (OfflineSalesRawDto.LineItemRawDto itemDto : dto.getLineItems()) {
            if (itemDto == null) {
                continue;
            }
            String title = resolveItemTitle(itemDto);
            if (title == null || title.isBlank()) {
                continue;
            }
            int qty = itemDto.getQuantity() != null ? itemDto.getQuantity() : 0;
            contribution.merge(title, qty, Integer::sum);
        }
        return contribution;
    }

    private Map<String, Integer> aggregateByTitle(List<OfflineSalesItem> items) {
        Map<String, Integer> contribution = new HashMap<>();
        for (OfflineSalesItem item : items) {
            if (item == null || item.getTitle() == null || item.getTitle().isBlank()) {
                continue;
            }
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            contribution.merge(item.getTitle(), qty, Integer::sum);
        }
        return contribution;
    }

    private OfflineSalesInfo upsertOfflineSalesInfo(OfflineSalesRawDto dto) {
        List<OfflineSalesInfo> existingList = offlineSalesInfoRepository.findAllByOrderId(dto.getId());
        if (existingList.isEmpty()) {
            return offlineSalesInfoRepository.save(toNewInfoEntity(dto));
        }
        if (existingList.size() > 1) {
            log.warn("중복 orderId 발견: orderId={}, count={}", dto.getId(), existingList.size());
        }
        OfflineSalesInfo info = existingList.get(0);
        updateFields(info, dto);
        return offlineSalesInfoRepository.save(info);
    }

    private void upsertOfflineSalesItem(
            OfflineSalesInfo info,
            OfflineSalesRawDto.LineItemRawDto itemDto,
            String lineItemId) {
        List<OfflineSalesItem> existingList = offlineSalesItemRepository.findAllByLineItemId(lineItemId);
        OfflineSalesItem item;
        if (existingList.isEmpty()) {
            item = toNewItemEntity(info, itemDto, lineItemId);
        } else {
            if (existingList.size() > 1) {
                log.warn("중복 lineItemId 발견: lineItemId={}, count={}", lineItemId, existingList.size());
                offlineSalesItemRepository.deleteAll(existingList.subList(1, existingList.size()));
            }
            item = existingList.get(0);
            updateItemFields(item, itemDto);
        }
        item.setOfflineSalesInfo(info);
        offlineSalesItemRepository.save(item);
    }

    private OfflineSalesInfo toNewInfoEntity(OfflineSalesRawDto dto) {
        OfflineSalesInfo.OfflineSalesInfoBuilder builder = OfflineSalesInfo.builder()
                .orderId(dto.getId())
                .orderState(dto.getOrderState())
                .orderNumber(dto.getOrderNumber())
                .createdAt(toLocalDateTime(dto.getCreatedAt()));
        applyChargePrice(builder, dto.getChargePrice());
        return builder.build();
    }

    private OfflineSalesInfo updateFields(OfflineSalesInfo existing, OfflineSalesRawDto dto) {
        existing.setOrderState(dto.getOrderState());
        existing.setOrderNumber(dto.getOrderNumber());
        existing.setCreatedAt(toLocalDateTime(dto.getCreatedAt()));
        if (dto.getChargePrice() != null) {
            existing.setListPrice(dto.getChargePrice().getListPrice());
            existing.setDiscountAmount(dto.getChargePrice().getDiscountAmount());
            existing.setTaxAmount(dto.getChargePrice().getTaxAmount());
            existing.setSupplyAmount(dto.getChargePrice().getSupplyAmount());
            existing.setTaxExemptAmount(dto.getChargePrice().getTaxExemptAmount());
            existing.setTotalAmount(dto.getChargePrice().getTotalAmount());
        }
        return existing;
    }

    private void applyChargePrice(OfflineSalesInfo.OfflineSalesInfoBuilder builder,
            OfflineSalesRawDto.ChargePriceRawDto chargePrice) {
        if (chargePrice == null) {
            return;
        }
        builder.listPrice(chargePrice.getListPrice())
                .discountAmount(chargePrice.getDiscountAmount())
                .taxAmount(chargePrice.getTaxAmount())
                .supplyAmount(chargePrice.getSupplyAmount())
                .taxExemptAmount(chargePrice.getTaxExemptAmount())
                .totalAmount(chargePrice.getTotalAmount());
    }

    private OfflineSalesItem toNewItemEntity(
            OfflineSalesInfo info,
            OfflineSalesRawDto.LineItemRawDto itemDto,
            String lineItemId) {
        OfflineSalesItem item = OfflineSalesItem.builder()
                .offlineSalesInfo(info)
                .lineItemId(lineItemId)
                .title(resolveItemTitle(itemDto))
                .priceUnit(itemDto.getItemPrice() != null ? itemDto.getItemPrice().getPriceUnit() : null)
                .priceValue(itemDto.getItemPrice() != null ? itemDto.getItemPrice().getPriceValue() : null)
                .category(resolveItemCategory(itemDto))
                .quantity(itemDto.getQuantity())
                .memo(itemDto.getMemo())
                .build();
        syncItemDiscounts(item, itemDto);
        return item;
    }

    private OfflineSalesItem updateItemFields(OfflineSalesItem existing, OfflineSalesRawDto.LineItemRawDto itemDto) {
        existing.setTitle(resolveItemTitle(itemDto));
        existing.setCategory(resolveItemCategory(itemDto));
        if (itemDto.getItemPrice() != null) {
            existing.setPriceUnit(itemDto.getItemPrice().getPriceUnit());
            existing.setPriceValue(itemDto.getItemPrice().getPriceValue());
        }
        existing.setQuantity(itemDto.getQuantity());
        existing.setMemo(itemDto.getMemo());
        syncItemDiscounts(existing, itemDto);
        return existing;
    }

    private String buildLineItemKey(String orderId, int index) {
        return orderId + "-" + index;
    }

    private String buildPaymentKey(String orderId, int index) {
        return orderId + "-payment-" + index;
    }

    private void syncPayments(OfflineSalesInfo info, OfflineSalesRawDto dto) {
        offlineSalesPaymentRepository.deleteAllByOfflineSalesInfoId(info.getId());
        if (dto.getPayments() == null) {
            return;
        }
        List<OfflineSalesPayment> newPayments = new ArrayList<>();
        List<OfflineSalesRawDto.PaymentRawDto> payments = dto.getPayments();
        for (int index = 0; index < payments.size(); index++) {
            OfflineSalesRawDto.PaymentRawDto paymentDto = payments.get(index);
            if (paymentDto == null) {
                continue;
            }
            if (paymentDto.getSourceType() == null && paymentDto.getAmount() == null) {
                continue;
            }
            newPayments.add(OfflineSalesPayment.builder()
                    .offlineSalesInfo(info)
                    .paymentKey(buildPaymentKey(dto.getId(), index))
                    .sourceType(paymentDto.getSourceType())
                    .amount(paymentDto.getAmount())
                    .taxAmount(paymentDto.getTaxAmount())
                    .supplyAmount(paymentDto.getSupplyAmount())
                    .taxExemptAmount(paymentDto.getTaxExemptAmount())
                    .build());
        }
        offlineSalesPaymentRepository.saveAll(newPayments);
    }

    private List<OfflineSalesPaymentDto> mapPayments(OfflineSalesInfo offlineSalesInfo) {
        if (offlineSalesInfo.getPayments() == null || offlineSalesInfo.getPayments().isEmpty()) {
            return List.of();
        }
        return offlineSalesInfo.getPayments().stream()
                .map(payment -> OfflineSalesPaymentDto.builder()
                        .sourceType(payment.getSourceType())
                        .amount(payment.getAmount())
                        .taxAmount(payment.getTaxAmount())
                        .supplyAmount(payment.getSupplyAmount())
                        .taxExemptAmount(payment.getTaxExemptAmount())
                        .build())
                .collect(Collectors.toList());
    }

    private String resolveItemTitle(OfflineSalesRawDto.LineItemRawDto itemDto) {
        if (itemDto.getItem() != null && itemDto.getItem().getTitle() != null) {
            return itemDto.getItem().getTitle();
        }
        if (itemDto.getItemPrice() != null) {
            return itemDto.getItemPrice().getTitle();
        }
        return null;
    }

    private String resolveItemCategory(OfflineSalesRawDto.LineItemRawDto itemDto) {
        if (itemDto.getItem() != null
                && itemDto.getItem().getCategory() != null
                && itemDto.getItem().getCategory().getTitle() != null) {
            return itemDto.getItem().getCategory().getTitle();
        }
        return null;
    }

    private void syncItemDiscounts(OfflineSalesItem item, OfflineSalesRawDto.LineItemRawDto itemDto) {
        item.getDiscounts().clear();
        if (itemDto.getAppliedDiscounts() == null) {
            return;
        }
        for (OfflineSalesRawDto.LineItemRawDto.AppliedDiscounts discountDto
                : itemDto.getAppliedDiscounts()) {
            if (discountDto == null) {
                continue;
            }
            if (discountDto.getTitle() == null && discountDto.getAmount() == null) {
                continue;
            }
            item.getDiscounts().add(OfflineSalesItemDiscount.builder()
                    .offlineSalesItem(item)
                    .title(discountDto.getTitle())
                    .amount(discountDto.getAmount())
                    .build());
        }
    }

    private List<OfflineSalesAppliedDiscountDto> mapAppliedDiscounts(OfflineSalesItem offlineSalesItem) {
        if (offlineSalesItem.getDiscounts() == null || offlineSalesItem.getDiscounts().isEmpty()) {
            return List.of();
        }
        return offlineSalesItem.getDiscounts().stream()
                .map(discount -> OfflineSalesAppliedDiscountDto.builder()
                        .title(discount.getTitle())
                        .amount(discount.getAmount())
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, KST) : null;
    }

    private void persistSyncStateSuccess() {
        transactionTemplate.executeWithoutResult(status -> updateSyncStateSuccess());
    }

    private void persistSyncStateError(Exception e) {
        transactionTemplate.executeWithoutResult(status -> updateSyncStateError(e));
    }

    private void updateSyncStateSuccess() {
        OfflineSalesSyncState syncState = offlineSalesSyncStateRepository.findById(1L)
                .orElseGet(() -> OfflineSalesSyncState.builder().id(1L).build());
        LocalDateTime now = LocalDateTime.now();
        syncState.setLastSyncedAt(now);
        syncState.setLastSuccessAt(now);
        syncState.setLastErrorMessage(null);
        offlineSalesSyncStateRepository.save(syncState);
    }

    private void updateSyncStateError(Exception e) {
        OfflineSalesSyncState syncState = offlineSalesSyncStateRepository.findById(1L)
                .orElseGet(() -> OfflineSalesSyncState.builder().id(1L).build());
        syncState.setLastSyncedAt(LocalDateTime.now());
        syncState.setLastErrorMessage(e.getMessage());
        offlineSalesSyncStateRepository.save(syncState);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfflineSalesInfoDto> getOfflineSalesList(Pageable pageable) {
        return offlineSalesInfoRepository.findAll(pageable)
                .map(offlineSalesInfo -> OfflineSalesInfoDto.builder()
                        .id(offlineSalesInfo.getId())
                        .orderId(offlineSalesInfo.getOrderId())
                        .orderState(offlineSalesInfo.getOrderState())
                        .orderNumber(offlineSalesInfo.getOrderNumber())
                        .createdAt(offlineSalesInfo.getCreatedAt())
                        .listPrice(offlineSalesInfo.getListPrice())
                        .discountAmount(offlineSalesInfo.getDiscountAmount())
                        .taxAmount(offlineSalesInfo.getTaxAmount())
                        .supplyAmount(offlineSalesInfo.getSupplyAmount())
                        .taxExemptAmount(offlineSalesInfo.getTaxExemptAmount())
                        .totalAmount(offlineSalesInfo.getTotalAmount())
                        .lineItems(mapLineItems(offlineSalesInfo))
                        .payments(mapPayments(offlineSalesInfo))
                        .build());
    }

    private List<OfflineSalesItemDto> mapLineItems(OfflineSalesInfo offlineSalesInfo) {
        if (offlineSalesInfo.getLineItems() == null) {
            return List.of();
        }
        return offlineSalesInfo.getLineItems().stream()
                .map(offlineSalesItem -> OfflineSalesItemDto.builder()
                        .id(offlineSalesItem.getId())
                        .orderId(offlineSalesInfo.getOrderId())
                        .title(offlineSalesItem.getTitle())
                        .category(offlineSalesItem.getCategory())
                        .priceUnit(offlineSalesItem.getPriceUnit())
                        .priceValue(offlineSalesItem.getPriceValue())
                        .quantity(offlineSalesItem.getQuantity())
                        .memo(offlineSalesItem.getMemo())
                        .appliedDiscounts(mapAppliedDiscounts(offlineSalesItem))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfflineSalesSummaryDto> getOfflineSalesSummary(
            OfflineSalesSummaryPeriod period,
            Pageable pageable) {
        Page<SalesSummaryProjection> summaryPage = switch (period) {
            case DAILY -> offlineSalesInfoRepository.dailySalesSummaryAndCompleted(pageable);
            case WEEKLY -> offlineSalesInfoRepository.weeklySalesSummaryAndCompleted(pageable);
            case MONTHLY -> offlineSalesInfoRepository.monthlySalesSummaryAndCompleted(pageable);
            case QUARTER -> offlineSalesInfoRepository.quarterlySalesSummaryAndCompleted(pageable);
            case YEARLY -> offlineSalesInfoRepository.yearlySalesSummaryAndCompleted(pageable);
            case TOTAL -> offlineSalesInfoRepository.totalSalesSummaryAndCompleted(pageable);
        };

        Map<LocalDate, List<SalesSummaryItemProjection>> itemsByPeriod = groupItemsByPeriod(
                fetchItemSummaries(period, summaryPage.getContent()));
        Map<LocalDate, List<PaymentSummaryProjection>> paymentsByPeriod = groupPaymentsByPeriod(
                fetchPaymentSummaries(period, summaryPage.getContent()));

        return summaryPage.map(projection -> toOfflineSalesSummaryDto(
                projection,
                itemsByPeriod.getOrDefault(projection.getPeriodStart(), List.of()),
                paymentsByPeriod.getOrDefault(projection.getPeriodStart(), List.of())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfflineSalesSummaryDto> getOfflineSalesSummaryReport(
            OfflineSalesSummaryPeriod period,
            LocalDate periodStart) {
        Optional<SalesSummaryProjection> projection = findSummaryProjection(period, periodStart);
        if (projection.isEmpty()) {
            return Optional.empty();
        }
        List<SalesSummaryItemProjection> items = fetchItemSummaries(
                period,
                List.of(projection.get()));
        List<PaymentSummaryProjection> payments = fetchPaymentSummaries(
                period,
                List.of(projection.get()));
        return Optional.of(toOfflineSalesSummaryDto(projection.get(), items, payments));
    }

    private Optional<SalesSummaryProjection> findSummaryProjection(
            OfflineSalesSummaryPeriod period,
            LocalDate periodStart) {
        return switch (period) {
            case DAILY -> offlineSalesInfoRepository.dailySalesSummaryByPeriodStart(
                    periodStart, OfflineSalesInfo.ORDER_STATE_COMPLETED);
            case WEEKLY -> offlineSalesInfoRepository.weeklySalesSummaryByPeriodStart(periodStart);
            case MONTHLY -> offlineSalesInfoRepository.monthlySalesSummaryByPeriodStart(periodStart);
            case QUARTER -> offlineSalesInfoRepository.quarterlySalesSummaryByPeriodStart(periodStart);
            case YEARLY -> offlineSalesInfoRepository.yearlySalesSummaryByPeriodStart(periodStart);
            case TOTAL -> offlineSalesInfoRepository.totalSalesSummaryAndCompleted(PageRequest.of(0, 1))
                    .stream()
                    .findFirst();
        };
    }

    private List<SalesSummaryItemProjection> fetchItemSummaries(
            OfflineSalesSummaryPeriod period,
            List<SalesSummaryProjection> summaries) {
        if (summaries.isEmpty()) {
            return List.of();
        }
        return switch (period) {
            case TOTAL -> offlineSalesItemRepository.totalItemSummary();
            case DAILY, WEEKLY, MONTHLY, QUARTER, YEARLY -> {
                List<LocalDate> periodStarts = summaries.stream()
                        .map(SalesSummaryProjection::getPeriodStart)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (periodStarts.isEmpty()) {
                    yield List.of();
                }
                yield switch (period) {
                    case DAILY -> offlineSalesItemRepository.dailyItemSummaryByPeriodStarts(periodStarts);
                    case WEEKLY -> offlineSalesItemRepository.weeklyItemSummaryByPeriodStarts(periodStarts);
                    case MONTHLY -> offlineSalesItemRepository.monthlyItemSummaryByPeriodStarts(periodStarts);
                    case QUARTER -> offlineSalesItemRepository.quarterlyItemSummaryByPeriodStarts(periodStarts);
                    case YEARLY -> offlineSalesItemRepository.yearlyItemSummaryByPeriodStarts(periodStarts);
                    default -> List.of();
                };
            }
        };
    }

    private List<PaymentSummaryProjection> fetchPaymentSummaries(
            OfflineSalesSummaryPeriod period,
            List<SalesSummaryProjection> summaries) {
        if (summaries.isEmpty()) {
            return List.of();
        }
        return switch (period) {
            case TOTAL -> offlineSalesPaymentRepository.totalPaymentSummary();
            case DAILY, WEEKLY, MONTHLY, QUARTER, YEARLY -> {
                List<LocalDate> periodStarts = summaries.stream()
                        .map(SalesSummaryProjection::getPeriodStart)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (periodStarts.isEmpty()) {
                    yield List.of();
                }
                yield switch (period) {
                    case DAILY -> offlineSalesPaymentRepository.dailyPaymentSummaryByPeriodStarts(
                            periodStarts, OfflineSalesInfo.ORDER_STATE_COMPLETED);
                    case WEEKLY -> offlineSalesPaymentRepository.weeklyPaymentSummaryByPeriodStarts(periodStarts);
                    case MONTHLY -> offlineSalesPaymentRepository.monthlyPaymentSummaryByPeriodStarts(periodStarts);
                    case QUARTER -> offlineSalesPaymentRepository.quarterlyPaymentSummaryByPeriodStarts(periodStarts);
                    case YEARLY -> offlineSalesPaymentRepository.yearlyPaymentSummaryByPeriodStarts(periodStarts);
                    default -> List.of();
                };
            }
        };
    }

    private Map<LocalDate, List<SalesSummaryItemProjection>> groupItemsByPeriod(
            List<SalesSummaryItemProjection> items) {
        return items.stream()
                .collect(Collectors.groupingBy(SalesSummaryItemProjection::getPeriodStart));
    }

    private Map<LocalDate, List<PaymentSummaryProjection>> groupPaymentsByPeriod(
            List<PaymentSummaryProjection> payments) {
        return payments.stream()
                .collect(Collectors.groupingBy(PaymentSummaryProjection::getPeriodStart));
    }

    private OfflineSalesSummaryDto toOfflineSalesSummaryDto(
            SalesSummaryProjection projection,
            List<SalesSummaryItemProjection> items,
            List<PaymentSummaryProjection> payments) {
        return OfflineSalesSummaryDto.builder()
                .periodStart(projection.getPeriodStart())
                .totalOrderCount(projection.getTotalOrderCount())
                .totalOrderAmount(projection.getTotalOrderAmount())
                .totalDiscountAmount(projection.getTotalDiscountAmount())
                .totalTaxAmount(projection.getTotalTaxAmount())
                .totalSupplyAmount(projection.getTotalSupplyAmount())
                .totalTaxExemptAmount(projection.getTotalTaxExemptAmount())
                .totalTotalAmount(projection.getTotalTotalAmount())
                .items(items.stream().map(this::toOfflineSalesSummaryItemDto).toList())
                .payments(payments.stream().map(this::toOfflineSalesSummaryPaymentDto).toList())
                .build();
    }

    private OfflineSalesSummaryPaymentDto toOfflineSalesSummaryPaymentDto(PaymentSummaryProjection projection) {
        return OfflineSalesSummaryPaymentDto.builder()
                .sourceType(projection.getSourceType())
                .paymentCount(projection.getPaymentCount())
                .totalAmount(projection.getTotalAmount())
                .totalTaxAmount(projection.getTotalTaxAmount())
                .totalSupplyAmount(projection.getTotalSupplyAmount())
                .totalTaxExemptAmount(projection.getTotalTaxExemptAmount())
                .build();
    }

    private OfflineSalesSummaryItemDto toOfflineSalesSummaryItemDto(SalesSummaryItemProjection projection) {
        return OfflineSalesSummaryItemDto.builder()
                .category(projection.getCategory())
                .title(projection.getTitle())
                .totalQuantity(projection.getTotalQuantity())
                .totalPriceValue(projection.getTotalPriceValue())
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public Page<OfflineSalesInfoDto> getOfflineSalesList(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return offlineSalesInfoRepository.findAllByCreatedAtBetween(startDate, endDate, pageable)
                .map(offlineSalesInfo -> OfflineSalesInfoDto.builder()
                        .id(offlineSalesInfo.getId())
                        .orderId(offlineSalesInfo.getOrderId())
                        .orderState(offlineSalesInfo.getOrderState())
                        .orderNumber(offlineSalesInfo.getOrderNumber())
                        .createdAt(offlineSalesInfo.getCreatedAt())
                        .listPrice(offlineSalesInfo.getListPrice())
                        .discountAmount(offlineSalesInfo.getDiscountAmount())
                        .taxAmount(offlineSalesInfo.getTaxAmount())
                        .supplyAmount(offlineSalesInfo.getSupplyAmount())
                        .taxExemptAmount(offlineSalesInfo.getTaxExemptAmount())
                        .totalAmount(offlineSalesInfo.getTotalAmount())
                        .lineItems(mapLineItems(offlineSalesInfo))
                        .payments(mapPayments(offlineSalesInfo))
                        .build());
    }
}
