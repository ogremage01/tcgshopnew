package com.shop.auth.refresh.scheduler;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shop.auth.refresh.service.RefreshTokenService;
import com.shop.log.sync.event.SyncLogEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component // Spring bean으로 등록
@RequiredArgsConstructor
@Slf4j
public class TokenScheduler {

    private static final String TOKEN_LOG_SOURCE = "scheduler.auth.refresh";

    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;

    // 리프레시 토큰 삭제
    /**
     * 리프레시 토큰 삭제
     * 
     * @param now 현재 시간
     */
    @Scheduled(cron = "0 0 */3 * * *") // every 3 hours
    public void deleteExpiredTokens() {
        LocalDateTime startTime = LocalDateTime.now();
        String result = "failure";
        String message = "";
        try {
            refreshTokenService.deleteExpiredTokens();
            result = "success";
        } catch (Exception e) {
            message = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : e.getClass().getSimpleName();
            log.error("Refresh token cleanup scheduler failed", e);
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            eventPublisher.publishEvent(
                    new SyncLogEvent("refresh-token", TOKEN_LOG_SOURCE, startTime, endTime, result, message));
        }
    }

}
