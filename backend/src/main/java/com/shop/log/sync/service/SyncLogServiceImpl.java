package com.shop.log.sync.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.shop.log.sync.dto.LastSyncTimesDto;
import com.shop.log.sync.dto.SyncLogDto;
import com.shop.log.sync.entity.SyncLog;
import com.shop.log.sync.repository.SyncLogRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class SyncLogServiceImpl implements SyncLogService {

    private static final int MAX_SYNC_LOG_MESSAGE_LENGTH = 20_000;

    private final SyncLogRepository syncLogRepository;

    @Override
    public void saveSyncLog(SyncLog syncLog) {
        if (syncLog != null) {
            syncLog.setMessage(truncateMessage(syncLog.getMessage()));
        }
        syncLogRepository.save(syncLog);
    }

    private String truncateMessage(String message) {
        if (message == null || message.length() <= MAX_SYNC_LOG_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_SYNC_LOG_MESSAGE_LENGTH) + "...(truncated)";
    }

    @Override
    public Page<SyncLogDto> getSyncLogs(Pageable pageable) {
        return syncLogRepository.findAll(pageable).map(SyncLog::toDto);
    }

    @Override
    public Page<SyncLogDto> findAllBySyncTargetOrderByStartTimeDesc(String syncTarget, Pageable pageable) {
        return syncLogRepository.findAllBySyncTargetOrderByStartTimeDesc(syncTarget, pageable).map(SyncLog::toDto);
    }

    @Override
    public Optional<SyncLogDto> findTopBySyncTargetOrderByStartTimeDesc(String syncTarget) {
        return syncLogRepository.findTopBySyncTargetOrderByStartTimeDesc(syncTarget).map(SyncLog::toDto);
    }

    @Override
    public LastSyncTimesDto getLastSyncTimes() {
        return LastSyncTimesDto.builder()
                .lastPriceSyncTime(
                        findTopBySyncTargetOrderByStartTimeDesc("prices").orElse(null))
                .lastMetadataSyncTime(
                        findTopBySyncTargetOrderByStartTimeDesc("metadata").orElse(null))
                .lastOpenBinderSyncTime(
                        findTopBySyncTargetOrderByStartTimeDesc("openbinder").orElse(null))
                .lastImageDownloadSyncTime(
                        findTopBySyncTargetOrderByStartTimeDesc("tcg-p-image-download").orElse(null))
                .lastPriceLinkOverwriteSyncTime(
                        findTopBySyncTargetOrderByStartTimeDesc("price-link-overwrite").orElse(null))
                .build();
    }

}
