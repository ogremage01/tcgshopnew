package com.shop.scheduler.price.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.shop.log.sync.event.SyncLogEvent;
import com.shop.product.metadata.repository.TcgPSetNameRepository;
import com.shop.product.metadata.repository.TcgPSyncGameRepository;
import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;
import com.shop.scheduler.price.application.selling.CardSellingPriceUpdateService;
import com.shop.scheduler.price.service.union.UnionPriceIngestionService;
import com.shop.scheduler.price.source.openbinder.OpenBinderSyncService;
import com.shop.scheduler.price.source.tcgplayer.TcgPlayerSyncResult;
import com.shop.scheduler.price.source.tcgplayer.TcgPlayerSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceServiceImpl implements PriceService {

    private static final String TCG_LOG_SOURCE = "external-price-source";
    private static final String OPEN_BINDER_LOG_SOURCE = "external-market-price";

    private final TcgPSetNameRepository setNameRepository;
    private final TcgPSyncGameRepository tcgPSyncGameRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TcgPlayerSyncService tcgPlayerSyncService;
    private final OpenBinderSyncService openBinderSyncService;
    private final UnionPriceIngestionService unionPriceIngestionService;
    private final CardSellingPriceUpdateService cardSellingPriceUpdateService;

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private final AtomicBoolean openBinderSyncRunning = new AtomicBoolean(false);
    private volatile CompletableFuture<Void> priceSyncTask;
    private volatile CompletableFuture<Void> openBinderSyncTask;

    /**
     * 동기화 대상 세트·타입 페어 목록(평탄 리스트). 이후 {@link TcgPlayerSyncService#buildSyncPlan(List)}로 라인별 묶음.
     */
    @Override
    public List<SetNameProductTypePairDto> getTargetSetNameAndTypeIds() {
        List<Long> productLineIds = tcgPSyncGameRepository.findAllProductLineIds();
        List<SetNameProductTypePairDto> syncGamePairs = setNameRepository
                .findSetNameAndProductTypeIdsByProductLineIdsOrderByReleaseDateDesc(productLineIds)
                .stream()
                .map(row -> new SetNameProductTypePairDto(row.getProductLineId(), row.getSetNameId(),
                        row.getProductTypeId()))
                .toList();
        List<SetNameProductTypePairDto> magicSealedPairs = setNameRepository
                .findSetNameAndProductTypeIdsByCategoryIdForMagicOrderByReleaseDateDesc(1L)
                .stream()
                .map(row -> new SetNameProductTypePairDto(row.getProductLineId(), row.getSetNameId(),
                        row.getProductTypeId()))
                .toList();
        return mergeDistinctPairsBySetNameAndType(syncGamePairs, magicSealedPairs);
    }

    /** setNameId + productTypeId 기준 중복 제거. 우선순위: 동기화 게임 쿼리 → Magic Sealed 보조 쿼리. */
    private static List<SetNameProductTypePairDto> mergeDistinctPairsBySetNameAndType(
            List<SetNameProductTypePairDto> syncGamePairs,
            List<SetNameProductTypePairDto> magicSealedPairs) {
        LinkedHashMap<String, SetNameProductTypePairDto> merged = new LinkedHashMap<>();
        for (SetNameProductTypePairDto setTypePair : syncGamePairs) {
            merged.putIfAbsent(pairKey(setTypePair), setTypePair);
        }
        for (SetNameProductTypePairDto setTypePair : magicSealedPairs) {
            merged.putIfAbsent(pairKey(setTypePair), setTypePair);
        }
        return new ArrayList<>(merged.values());
    }

    private static String pairKey(SetNameProductTypePairDto setTypePair) {
        return setTypePair.getSetNameId() + ":" + setTypePair.getProductTypeId();
    }

    @Override
    public Map<Long, List<SetNameProductTypePairDto>> getTargetSyncPlan() {
        return tcgPlayerSyncService.buildSyncPlan(getTargetSetNameAndTypeIds());
    }

    @Override
    public boolean startSyncPrices() {
        if (!syncRunning.compareAndSet(false, true)) {
            log.warn("TcgPPrice sync is already running. Skip this request.");
            return false;
        }
        LocalDateTime startTime = LocalDateTime.now();
        priceSyncTask = CompletableFuture.runAsync(() -> {
            String result = "failure";
            String syncMessage = "";
            try {
                TcgPlayerSyncResult outcome = tcgPlayerSyncService.syncAllProductLines(getTargetSyncPlan());
                result = outcome.result();
                syncMessage = outcome.message() != null ? outcome.message() : "";
            } catch (Exception e) {
                log.error("TcgPPrice sync failed with an unexpected exception", e);
                syncMessage = e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
            } finally {
                LocalDateTime endTime = LocalDateTime.now();
                publishSyncLog(startTime, endTime, result, syncMessage);
                syncRunning.set(false);
                priceSyncTask = null;
            }
            if (!"failure".equalsIgnoreCase(result)) {
                try {
                    unionPriceIngestionService.saveAllPricesToUnion();
                } catch (Exception e) {
                    log.error("Post-TCG-sync union price ingestion failed", e);
                }
                try {
                    cardSellingPriceUpdateService.updateCardCalculatedLinkedPrice();
                } catch (Exception e) {
                    log.error("Post-TCG-sync card selling price update failed", e);
                }
            }
        });
        return true;
    }

    @Override
    public boolean startSyncPricesWithPriority() {
        if (syncRunning.get()) {
            log.warn("Scheduler-priority requested: cancelling running TcgPPrice sync first.");
            CompletableFuture<Void> runningTask = priceSyncTask;
            if (runningTask != null) {
                runningTask.cancel(true);
            }
            while (syncRunning.get()) {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted while waiting previous TcgPPrice sync to stop.");
                    return false;
                }
            }
        }
        return startSyncPrices();
    }

    @Override
    public boolean isSyncRunning() {
        return syncRunning.get();
    }

    @Override
    public String syncPrices(List<SetNameProductTypePairDto> flatPairs) {
        return tcgPlayerSyncService.syncAllProductLines(tcgPlayerSyncService.buildSyncPlan(flatPairs)).result();
    }

    @Override
    public String syncPricesForProductLine(Long productLineId) {
        return tcgPlayerSyncService.syncProductLine(productLineId).result();
    }

    @Override
    public boolean startSyncOpenBinderPrices() {
        if (!openBinderSyncRunning.compareAndSet(false, true)) {
            log.warn("Open Binder price sync is already running. Skip this request.");
            return false;
        }
        openBinderSyncTask = CompletableFuture.runAsync(() -> {
            String result = "failure";
            try {
                result = syncOpenBinderPrices();
            } finally {
                openBinderSyncRunning.set(false);
                openBinderSyncTask = null;
            }
            if (!"failure".equalsIgnoreCase(result)) {
                try {
                    cardSellingPriceUpdateService.updateCardCalculatedLinkedPrice();
                } catch (Exception e) {
                    log.error("Post-OB-sync card selling price update failed", e);
                }
            }
        });
        return true;
    }

    @Override
    public boolean isOpenBinderSyncRunning() {
        return openBinderSyncRunning.get();
    }

    @Override
    public String syncOpenBinderPrices() {
        LocalDateTime startTime = LocalDateTime.now();
        String result = "failure";
        String message = "";
        try {
            var outcome = openBinderSyncService.syncAll();
            result = outcome.result();
            message = outcome.message();
        } catch (Exception e) {
            log.error("Open Binder sync failed with an unexpected exception", e);
            message = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            publishOpenBinderSyncLog(startTime, endTime, result, message);
        }
        saveUnionPricesAfterOpenBinder(result);
        return result;
    }

    /**
     * Open Binder 동기화가 일부라도 성공하면 당일 갱신된 tcg_p_prices·fab/mtg_prices를
     * union_prices·ProductSearchMap에 반영한다. (TCG → FAB/MTG 순, 동일 check_code는 뒤 단계가 덮어씀)
     */
    private void saveUnionPricesAfterOpenBinder(String openBinderResult) {
        if (!shouldSaveUnionAfterOpenBinder(openBinderResult)) {
            log.warn("Skip union price save after Open Binder sync. result={}", openBinderResult);
            return;
        }
        try {
            log.info("Starting full union price save after daily price sync (result={})", openBinderResult);
            unionPriceIngestionService.saveAllPricesToUnion();
            log.info("Full union price save after daily price sync completed.");
        } catch (Exception e) {
            log.error("Full union price save after daily price sync failed", e);
        }
    }

    private static boolean shouldSaveUnionAfterOpenBinder(String openBinderResult) {
        if (openBinderResult == null || openBinderResult.isBlank()) {
            return false;
        }
        String normalized = openBinderResult.trim().toLowerCase();
        return "success".equals(normalized) || normalized.startsWith("partial_success");
    }

    @Override
    public String syncOpenBinderPricesWithPriority() {
        if (openBinderSyncRunning.get()) {
            log.warn("Scheduler-priority requested: cancelling running Open Binder sync first.");
            CompletableFuture<Void> runningTask = openBinderSyncTask;
            if (runningTask != null) {
                runningTask.cancel(true);
            }
            while (openBinderSyncRunning.get()) {
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "failure";
                }
            }
        }
        return syncOpenBinderPrices();
    }

    @Override
    public String syncMtgPrices() {
        return openBinderSyncService.syncMtgPrices();
    }

    @Override
    public String syncFabPrices() {
        return openBinderSyncService.syncFabPrices();
    }

    private void publishSyncLog(LocalDateTime startTime, LocalDateTime endTime, String result, String message) {
        String safeMessage = message != null ? message : "";
        eventPublisher.publishEvent(
                new SyncLogEvent("prices", TCG_LOG_SOURCE, startTime, endTime, result, safeMessage));
    }

    private void publishOpenBinderSyncLog(LocalDateTime startTime, LocalDateTime endTime, String result,
            String message) {
        String safeMessage = message != null ? message : "";
        eventPublisher.publishEvent(
                new SyncLogEvent("openbinder", OPEN_BINDER_LOG_SOURCE, startTime, endTime, result, safeMessage));
    }
}
