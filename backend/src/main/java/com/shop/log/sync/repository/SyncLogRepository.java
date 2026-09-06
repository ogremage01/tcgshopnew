package com.shop.log.sync.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.log.sync.entity.SyncLog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    // 동기화 로그 조회
    /**
     * 동기화 로그 조회
     * 
     * @param syncTarget 동기화 대상
     * @return 동기화 로그
     */
    Optional<SyncLog> findTopBySyncTargetOrderByStartTimeDesc(String syncTarget);

    // 동기화 로그 조회
    /**
     * 동기화 로그 조회
     * 
     * @param syncTarget 동기화 대상
     * @param pageable   페이지 정보
     * @return 동기화 로그
     */
    Page<SyncLog> findAllBySyncTargetOrderByStartTimeDesc(String syncTarget, Pageable pageable);

}
