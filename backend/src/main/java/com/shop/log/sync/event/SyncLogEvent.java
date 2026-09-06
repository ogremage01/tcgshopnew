package com.shop.log.sync.event;

import java.time.Duration;
import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class SyncLogEvent {

    // 동기화 로그 이벤트
    private final String syncSource;
    private final String syncTarget;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Duration proceedingTime;
    private final String result;
    private final String message;

    /**
     * 동기화 로그 이벤트 생성
     * 
     * @param syncSource     동기화 소스
     * @param syncTarget     동기화 대상
     * @param startTime      동기화 시작 시간
     * @param endTime        동기화 종료 시간
     * @param proceedingTime 동기화 실행 시간
     * @param result         동기화 결과
     * @param message        동기화 메시지
     */
    public SyncLogEvent(String syncTarget, String syncSource, LocalDateTime startTime, LocalDateTime endTime,
            String result, String message) {
        this.syncTarget = syncTarget;
        this.syncSource = syncSource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.proceedingTime = Duration.between(startTime, endTime);
        this.result = result;
        this.message = message;
    }
}
