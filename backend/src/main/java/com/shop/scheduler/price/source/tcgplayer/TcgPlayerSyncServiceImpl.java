package com.shop.scheduler.price.source.tcgplayer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.shop.card.entity.TcgPPrice;
import com.shop.log.sync.event.SyncLogEvent;
import com.shop.product.metadata.repository.TcgPSetNameRepository;
import com.shop.product.metadata.repository.TcgPSyncGameRepository;
import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;
import com.shop.scheduler.price.dto.PriceSyncDto;
import com.shop.scheduler.price.service.batch.PriceBatchSaver;
import com.shop.scheduler.price.source.client.PriceFetchClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TcgPlayerSyncServiceImpl implements TcgPlayerSyncService {
    private static final String TCG_LOG_SOURCE = "external-price-source";
    private final List<String> filterConditions = Arrays.asList("Near Mint", "Unopened");
    private final TcgPSetNameRepository setNameRepository;
    private final TcgPSyncGameRepository tcgPSyncGameRepository;
    private final PriceFetchClient fetchClient;
    private final PriceBatchSaver priceBatchSaver;
    private final ApplicationEventPublisher eventPublisher;

    private record SyncOutcome(boolean anySucceeded) {}
    private record SyncLineResult(SyncOutcome outcome, int pairSuccessCount) {}

    /**
     * 세트·상품타입 페어 목록을 productLineId 기준으로 묶습니다.
     *
     * @param flatPairs 동기화 게임에서 나온 페어 + Magic Sealed 보조 페어 등, 평탄화된 목록
     * @return productLineId → 해당 라인에서 동기화할 페어 목록
     */
    @Override
    public Map<Long, List<SetNameProductTypePairDto>> buildSyncPlan(List<SetNameProductTypePairDto> flatPairs) {
        Map<Long, List<SetNameProductTypePairDto>> syncPlanByProductLineId = new LinkedHashMap<>();
        for (SetNameProductTypePairDto setTypePair : flatPairs) {
            syncPlanByProductLineId
                    .computeIfAbsent(setTypePair.getProductLineId(), id -> new ArrayList<>())
                    .add(setTypePair);
        }
        return syncPlanByProductLineId;
    }

    /**
     * 모든 제품 라인에 대해 세트·타입 페어 동기화를 수행합니다.
     *
     * @param syncPlanByProductLineId {@link #buildSyncPlan(List)} 결과
     * @return 동기화 결과
     */
    @Override
    public TcgPlayerSyncResult syncAllProductLines(Map<Long, List<SetNameProductTypePairDto>> syncPlanByProductLineId) {

        LinkedHashSet<Long> productLineIds = productLineIdsInExecutionOrder(syncPlanByProductLineId);
        int totalPairCount = syncPlanByProductLineId.values().stream().mapToInt(List::size).sum();
        int successPairCount = 0;
        try {
            fetchClient.throttle();
            for (Long productLineId : productLineIds) {
                List<SetNameProductTypePairDto> pairsForProductLine =
                        syncPlanByProductLineId.getOrDefault(productLineId, List.of());
                SyncLineResult lineResult = syncPriceSets(pairsForProductLine);
                successPairCount += lineResult.pairSuccessCount();
            }
            String progress = formatPairSyncProgress(successPairCount, totalPairCount);
            String result = successPairCount == totalPairCount ? "success"
                    : (successPairCount > 0 ? "partial_success" : "failure");
            return new TcgPlayerSyncResult(result, progress);
        } catch (Exception e) {
            String progress = formatPairSyncProgress(successPairCount, totalPairCount);
            String detail = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName();
            String result = successPairCount > 0 ? "partial_success" : "failure";
            return new TcgPlayerSyncResult(result, joinProgressAndDetail(progress, detail));
        }
    }

    @Override
    public TcgPlayerSyncResult syncProductLine(Long productLineId) {
        if (productLineId == null) return new TcgPlayerSyncResult("failure", "productLineId is null");
        List<SetNameProductTypePairDto> pairsForProductLine = new ArrayList<>(setNameRepository
                .findSetNameAndProductTypeIdsByProductLineIdsOrderByReleaseDateDesc(List.of(productLineId))
                .stream()
                .map(row -> new SetNameProductTypePairDto(row.getProductLineId(), row.getSetNameId(), row.getProductTypeId()))
                .toList());
        List<SetNameProductTypePairDto> magicSealedPairs = setNameRepository
                .findSetNameAndProductTypeIdsByCategoryIdForMagicOrderByReleaseDateDesc(1L)
                .stream()
                .map(row -> new SetNameProductTypePairDto(row.getProductLineId(), row.getSetNameId(), row.getProductTypeId()))
                .toList();
        pairsForProductLine.addAll(magicSealedPairs);
        int totalPairCount = pairsForProductLine.size();
        try {
            fetchClient.throttle();
            SyncLineResult lineResult = syncPriceSets(pairsForProductLine);
            int successCount = lineResult.pairSuccessCount();
            String progress = formatPairSyncProgress(successCount, totalPairCount);
            String result = successCount == totalPairCount ? "success"
                    : (successCount > 0 ? "partial_success" : "failure");
            return new TcgPlayerSyncResult(result, progress);
        } catch (Exception e) {
            String detail = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName();
            return new TcgPlayerSyncResult("failure", joinProgressAndDetail(formatPairSyncProgress(0, totalPairCount), detail));
        }
    }

    private SyncLineResult syncPriceSets(List<SetNameProductTypePairDto> pairsToSync) {
        boolean anySucceeded = false;
        int pairSuccessCount = 0;
        for (SetNameProductTypePairDto setTypePair : pairsToSync) {
            LocalDateTime setStartTime = LocalDateTime.now();
            List<PriceSyncDto> priceDtos;
            try {
                priceDtos = fetchClient.fetchTcgPlayerPrices(setTypePair.getSetNameId(), setTypePair.getProductTypeId());
            } catch (WebClientResponseException.Forbidden e) {
                fetchClient.throttle();
                publishSetSyncLog(setTypePair, setStartTime, "failure", "403 Forbidden");
                continue;
            } catch (WebClientResponseException e) {
                fetchClient.throttle();
                String httpDetail = "HTTP " + e.getStatusCode().value();
                publishSetSyncLog(setTypePair, setStartTime, "failure", httpDetail);
                continue;
            } catch (WebClientRequestException e) {
                fetchClient.throttle();
                String detail = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName();
                publishSetSyncLog(setTypePair, setStartTime, "failure", detail);
                continue;
            }
            if (priceDtos == null) {
                fetchClient.throttle();
                publishSetSyncLog(setTypePair, setStartTime, "success", "null response");
                pairSuccessCount++;
                continue;
            }
            List<TcgPPrice> toSave = priceDtos.stream()
                    .filter(dto -> filterConditions.stream().anyMatch(Objects.toString(dto.getCondition(), "")::contains))
                    .map(PriceSyncDto::toEntity)
                    .toList();
            try {
                priceBatchSaver.saveTcgBatch(toSave);
            } catch (Exception e) {
                fetchClient.throttle();
                String detail = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : e.getClass().getSimpleName();
                publishSetSyncLog(setTypePair, setStartTime, "failure", detail);
                continue;
            }
            pairSuccessCount++;
            anySucceeded = true;
            publishSetSyncLog(setTypePair, setStartTime, "success", "");
            fetchClient.throttle();
        }
        return new SyncLineResult(new SyncOutcome(anySucceeded), pairSuccessCount);
    }

    private static String formatPairSyncProgress(int successPairCount, int totalPairCount) {
        if (totalPairCount <= 0) return "동기화 대상 세트·타입 페어 없음.";
        return String.format("세트·타입 페어 %d/%d건 동기화 완료.", successPairCount, totalPairCount);
    }

    private static String joinProgressAndDetail(String progress, String detail) {
        if (detail == null || detail.isBlank()) return progress;
        return progress + " " + detail;
    }

    private void publishSetSyncLog(SetNameProductTypePairDto setTypePair, LocalDateTime startTime, String result, String message) {
        eventPublisher.publishEvent(new SyncLogEvent(
                "price:set:" + setTypePair.getSetNameId(), TCG_LOG_SOURCE, startTime, LocalDateTime.now(), result, message));
    }

    private LinkedHashSet<Long> productLineIdsInExecutionOrder(
            Map<Long, List<SetNameProductTypePairDto>> syncPlanByProductLineId) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>(tcgPSyncGameRepository.findAllProductLineIds());
        ids.addAll(syncPlanByProductLineId.keySet());
        return ids;
    }
}
