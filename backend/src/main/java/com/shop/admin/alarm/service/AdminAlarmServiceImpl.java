package com.shop.admin.alarm.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.shop.admin.alarm.dto.AlarmDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminAlarmServiceImpl implements AdminAlarmService {

    private static final String HEARTBEAT_EVENT = "heartbeat";
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 60 * 60 * 1000L;

    @Override
    public SseEmitter subscribe(Long connectionId) {
        SseEmitter existing = emitters.remove(connectionId);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter sseEmitter = new SseEmitter(TIMEOUT);
        emitters.put(connectionId, sseEmitter);

        sseEmitter.onCompletion(() -> {
            emitters.remove(connectionId, sseEmitter);
            log.debug("알림 구독 종료: connectionId={}, active={}", connectionId, emitters.size());
        });
        sseEmitter.onTimeout(() -> {
            emitters.remove(connectionId, sseEmitter);
            log.debug("알림 구독 시간 초과: connectionId={}, active={}", connectionId, emitters.size());
        });
        sseEmitter.onError(e -> {
            emitters.remove(connectionId, sseEmitter);
            logClientDisconnect("알림 구독 종료", connectionId, e);
        });

        log.info("알림 구독 시작: connectionId={}, active={}", connectionId, emitters.size());

        try {
            sseEmitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(connectionId, sseEmitter);
            logClientDisconnect("알림 connect 전송 실패", connectionId, e);
            sseEmitter.complete();
        }

        return sseEmitter;
    }

    @Override
    public void send(AlarmDto alarmDto) {
        if (emitters.isEmpty()) {
            log.warn("알림 발송 건너뛰기: 접속 중인 admin SSE 없음");
            return;
        }

        log.info("알림 발송 시작: active={}, title={}", emitters.size(), alarmDto.getTitle());

        emitters.forEach((connectionId, sseEmitter) -> {
            try {
                sseEmitter.send(SseEmitter.event().name("alarm").data(alarmDto));
            } catch (Exception e) {
                emitters.remove(connectionId, sseEmitter);
                logClientDisconnect("알림 발송 실패", connectionId, e);
                sseEmitter.complete();
            }
        });

        log.info("알림 발송 완료: active={}", emitters.size());
    }

    @Scheduled(fixedDelay = 30_000L)
    public void heartbeat() {
        emitters.forEach((connectionId, sseEmitter) -> {
            try {
                sseEmitter.send(SseEmitter.event().name(HEARTBEAT_EVENT).data("ping"));
            } catch (Exception e) {
                emitters.remove(connectionId, sseEmitter);
                logClientDisconnect("알림 heartbeat 실패", connectionId, e);
                sseEmitter.complete();
            }
        });
    }

    /**
     * 브라우저 새로고침·탭 닫기·재연결 시 클라이언트가 먼저 끊으면 발생한다. 인증/서버 장애가 아니다.
     */
    private void logClientDisconnect(String message, Long connectionId, Throwable e) {
        if (isClientDisconnect(e)) {
            log.debug("{}: connectionId={}, reason=client disconnected", message, connectionId);
            return;
        }
        log.warn("{}: connectionId={}", message, connectionId, e);
    }

    private static boolean isClientDisconnect(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ClientAbortException
                    || current instanceof AsyncRequestNotUsableException) {
                return true;
            }
            String msg = current.getMessage();
            if (msg != null && (msg.contains("중단") || msg.contains("abort"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
