package com.shop.scheduler.offline.task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shop.offline.product.service.OfflineProductService;
import com.shop.offline.sales.service.OfflineSalesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineDataDownloadScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OfflineProductService offlineProductService;
    private final OfflineSalesService offlineSalesService;

    // 매일 1시간마다 오프라인 상품 다운로드
    @Scheduled(cron = "0 0 * * * *")
    public void downloadOfflineProductsScheduled() {
        offlineProductService.downloadOfflineProducts();
    }

    // 매일 새벽 12시(KST) 오프라인 매출 다운로드 (전일 기준 한 달 전 ~ 전일 23:59:59)
    @Scheduled(cron = "0 0 0 * * *", zone = "${app.scheduler.zone:Asia/Seoul}")
    public void downloadOfflineSalesScheduled() {
        LocalDate endDate = LocalDate.now(KST).minusDays(1);
        LocalDateTime startDate = endDate.minusMonths(1).atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        log.info("Offline sales download scheduler triggered. period={} ~ {}", startDate, endDateTime);
        try {
            boolean started = offlineSalesService.downloadOfflineSalesbyPeriod(startDate, endDateTime);
            if (!started) {
                log.warn("Offline sales download skipped — already running. period={} ~ {}", startDate, endDateTime);
                return;
            }
            log.info("Offline sales download completed. period={} ~ {}", startDate, endDateTime);
        } catch (Exception e) {
            log.error("Offline sales download failed. period={} ~ {}", startDate, endDateTime, e);
        }
    }
}
