package com.shop.scheduler.image.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shop.scheduler.image.application.TcgPImageDownloadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TCGPlayer 카드 이미지 다운로드 전용 스케줄.
 * CardDataUpdateScheduler(메타·가격·UnionPrice)와 분리해 앞 단계 실패 시에도 실행된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcgPImageDownloadScheduler {

    private final TcgPImageDownloadService tcgPImageDownloadService;

    @Scheduled(cron = "${app.scheduler.tcg-p-image-download:0 30 3 * * *}", zone = "${app.scheduler.zone:Asia/Seoul}")
    public void downloadTcgPImagesScheduled() {
        log.info("TCG-P image download scheduler triggered.");
        if (tcgPImageDownloadService.isDownloadRunning()) {
            log.warn("TCG-P image download already running. Skip scheduled run.");
            return;
        }
        boolean started = tcgPImageDownloadService.downloadTcgPImages();
        if (!started) {
            log.warn("TCG-P image download did not start (already running).");
        }
    }
}
