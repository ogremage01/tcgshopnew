package com.shop.log.sync.service;

import com.shop.log.sync.dto.SyncLogDto;
import com.shop.log.sync.entity.SyncLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import com.shop.log.sync.dto.LastSyncTimesDto;

public interface SyncLogService {

    /**
     * 동기화 로그를 저장한다.
     * 
     * @param syncLogDto 동기화 로그 정보
     */
    void saveSyncLog(SyncLog syncLog);

    /**
     * 동기화 로그를 조회한다.
     * 
     * @param pageable 페이지 정보(startTime 기준 내림차순)
     * @return 동기화 로그 목록
     */
    Page<SyncLogDto> getSyncLogs(Pageable pageable);

    /**
     * 가장 최근 동기화 로그를 조회한다.
     * 
     * @param syncTarget 동기화 대상
     * @return 가장 최근 동기화 로그
     */
    Optional<SyncLogDto> findTopBySyncTargetOrderByStartTimeDesc(String syncTarget);

    /**
     * 동기화 대상별 가장 최근 동기화 로그를 조회한다.
     * 
     * @param syncTarget 동기화 대상
     * @param pageable   페이지 정보(startTime 기준 내림차순)
     * @return 동기화 대상별 가장 최근 동기화 로그
     */
    Page<SyncLogDto> findAllBySyncTargetOrderByStartTimeDesc(String syncTarget, Pageable pageable);

    /**
     * 동기화 대상별 가장 최근 동기 로그 (관리자 메타데이터 last-time API·집계용)
     */
    LastSyncTimesDto getLastSyncTimes();

}
