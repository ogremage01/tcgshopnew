package com.shop.scheduler.image.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shop.scheduler.image.application.ImageDownloadOrchestrationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * mtg-kr(OpenBinder) 이미지 다운로드 후 Scryfall 폴백을 순차로 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenBinderImageDownloadScheduler {

    private final ImageDownloadOrchestrationService imageDownloadOrchestrationService;

    @Scheduled(cron = "${app.scheduler.openbinder-image-download:0 30 3 * * *}", zone = "${app.scheduler.zone:Asia/Seoul}")
    public void downloadOpenBinderImagesScheduled() {
        log.info("Image download orchestration scheduler triggered.");
        if (imageDownloadOrchestrationService.isDownloadRunning()) {
            log.warn("Image download orchestration already running. Skip scheduled run.");
            return;
        }
        boolean started = imageDownloadOrchestrationService.downloadOpenBinderThenScryfall();
        if (!started) {
            log.warn("Image download orchestration did not start (already running).");
        }
    }
}
