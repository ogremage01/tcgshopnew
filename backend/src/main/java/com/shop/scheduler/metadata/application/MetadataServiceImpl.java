package com.shop.scheduler.metadata.application;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.log.sync.event.SyncLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본. 외부 카탈로그 연동 URL은 포함하지 않으며, 동기화는 생략됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataServiceImpl implements MetadataService {
    private final ApplicationEventPublisher eventPublisher;
    private static final String OMITTED_SOURCE = "omitted";
    private static final String OMITTED_MESSAGE = "external catalog source omitted in public snapshot";
    /**
     * 1: Magic: The Gathering
     * 62: Flesh and Blood
     * 79: Star Wars Unlimited
     * 71: Lorcana
     * 89: RiftBound
     */
    private static final List<Long> DEFAULT_CATEGORY_IDS = Arrays.asList(1L, 62L, 79L, 71L, 89L);
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private volatile Thread runningThread;

    @Override
    public Boolean syncAll() {
        if (!syncRunning.compareAndSet(false, true)) return false;
        cancelRequested.set(false);
        runningThread = Thread.currentThread();
        LocalDateTime allStartTime = LocalDateTime.now();
        List<SyncResult> subResults = new ArrayList<>();
        String allResult = "success";
        String allMessage = "";
        try {
            checkCancelled("metadata.syncAll.begin");
            log.warn("Portfolio snapshot: external catalog sync is omitted.");
            LocalDateTime productLinesStartTime = LocalDateTime.now();
            SyncResult productLinesSyncResult = syncProductLines("");
            saveSyncLogSafely("productLines", OMITTED_SOURCE,
                    productLinesStartTime, LocalDateTime.now(), productLinesSyncResult.result(), productLinesSyncResult.message());
            subResults.add(productLinesSyncResult);
            checkCancelled("metadata.afterProductLines");

            LocalDateTime productTypesStartTime = LocalDateTime.now();
            SyncResult productTypesSyncResult = syncProductTypes(DEFAULT_CATEGORY_IDS, "");
            saveSyncLogSafely("productTypes", OMITTED_SOURCE,
                    productTypesStartTime, LocalDateTime.now(), productTypesSyncResult.result(), productTypesSyncResult.message());
            subResults.add(productTypesSyncResult);
            checkCancelled("metadata.afterProductTypes");

            LocalDateTime setNamesStartTime = LocalDateTime.now();
            SyncResult setNamesSyncResult = syncSetNames(DEFAULT_CATEGORY_IDS, "");
            saveSyncLogSafely("setNames", OMITTED_SOURCE,
                    setNamesStartTime, LocalDateTime.now(), setNamesSyncResult.result(), setNamesSyncResult.message());
            subResults.add(setNamesSyncResult);
            checkCancelled("metadata.afterSetNames");

            boolean hasSuccess = subResults.stream().anyMatch(r -> "success".equals(r.result()));
            boolean hasFailure = subResults.stream().anyMatch(r -> "failure".equals(r.result()));
            if (hasSuccess && hasFailure) allResult = "partial_success";
            else if (hasFailure) allResult = "failure";
            return true;
        } catch (Exception e) {
            allResult = "failure";
            allMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return false;
        } finally {
            saveSyncLogSafely("metadata", OMITTED_SOURCE, allStartTime, LocalDateTime.now(), allResult, allMessage);
            syncRunning.set(false);
            runningThread = null;
            cancelRequested.set(false);
        }
    }

    @Override
    public Boolean syncAllWithPriority() {
        if (syncRunning.get()) {
            cancelRequested.set(true);
            Thread thread = runningThread;
            if (thread != null) thread.interrupt();
            while (syncRunning.get()) {
                try { Thread.sleep(200L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
            }
        }
        return syncAll();
    }

    private void checkCancelled(String stage) {
        if (cancelRequested.get() || Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("cancelled at " + stage);
        }
    }

    private void saveSyncLogSafely(String syncTarget, String syncSource, LocalDateTime startTime, LocalDateTime endTime,
            String result, String message) {
        eventPublisher.publishEvent(new SyncLogEvent(syncTarget, syncSource, startTime, endTime, result, message));
    }

    @Override
    @Transactional
    public SyncResult syncProductLines(String url) {
        return omittedCatalogResult();
    }

    @Override
    @Transactional
    public SyncResult syncSetNames(List<Long> categoryIds, String url) {
        return omittedCatalogResult();
    }

    @Override
    @Transactional
    public SyncResult syncProductTypes(List<Long> productLineIds, String url) {
        return omittedCatalogResult();
    }

    private static SyncResult omittedCatalogResult() {
        return new SyncResult("failure", OMITTED_MESSAGE);
    }
}
