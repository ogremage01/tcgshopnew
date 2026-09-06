package com.shop.log.sync.event;

import com.shop.log.sync.service.SyncLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.shop.log.sync.entity.SyncLog;

// 동기화 로그 이벤트 리스너
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLogEventListener {

    // 동기화 로그 이벤트 리스너
    // 이벤트 리스너란? 이벤트가 발생했을 때 이벤트 리스너가 이벤트를 처리하는 역할을 합니다.
    private final SyncLogService syncLogService;

    // 동기화 로그 이벤트 처리
    /**
     * 동기화 로그 이벤트 처리
     * 
     * @param event 동기화 로그 이벤트
     */
    @Async
    @EventListener
    public void handleSyncLogEvent(SyncLogEvent event) {
        try {
            syncLogService.saveSyncLog(new SyncLog(
                    null,
                    event.getSyncSource(),
                    event.getSyncTarget(),
                    event.getStartTime(),
                    event.getEndTime(),
                    event.getProceedingTime(),
                    event.getResult(),
                    event.getMessage()));
        } catch (Exception e) {
            log.error("Failed to save sync log. source={}, target={}", event.getSyncSource(), event.getSyncTarget(), e);
        }
    }
}
