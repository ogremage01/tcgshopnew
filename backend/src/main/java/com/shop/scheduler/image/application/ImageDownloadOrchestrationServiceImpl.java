package com.shop.scheduler.image.application;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageDownloadOrchestrationServiceImpl implements ImageDownloadOrchestrationService {

    private final OpenBinderImageDownloadService openBinderImageDownloadService;
    private final ScryfallImageDownloadService scryfallImageDownloadService;
    private final AtomicBoolean orchestrationRunning = new AtomicBoolean(false);

    @Override
    public boolean isDownloadRunning() {
        return orchestrationRunning.get()
                || openBinderImageDownloadService.isDownloadRunning()
                || scryfallImageDownloadService.isDownloadRunning();
    }

    @Async
    @Override
    public void downloadOpenBinderThenScryfallAsync() {
        log.info("Image download orchestration async request accepted.");
        downloadOpenBinderThenScryfall();
    }

    @Override
    public boolean downloadOpenBinderThenScryfall() {
        if (!orchestrationRunning.compareAndSet(false, true)) {
            log.warn("Image download orchestration already running. Skip.");
            return false;
        }
        try {
            if (openBinderImageDownloadService.isDownloadRunning()
                    || scryfallImageDownloadService.isDownloadRunning()) {
                log.warn("OpenBinder or Scryfall image download already running. Skip orchestration.");
                return false;
            }
            log.info("Image download orchestration started. OpenBinder then Scryfall.");
            boolean openBinderStarted = openBinderImageDownloadService.downloadOpenBinderImages();
            if (!openBinderStarted) {
                log.warn("OpenBinder image download did not start. Skip Scryfall fallback.");
                return false;
            }
            log.info("OpenBinder image download finished. Starting Scryfall fallback.");
            boolean scryfallStarted = scryfallImageDownloadService.downloadScryfallImages();
            if (!scryfallStarted) {
                log.warn("Scryfall image download did not start after OpenBinder (already running).");
            }
            log.info("Image download orchestration finished.");
            return true;
        } finally {
            orchestrationRunning.set(false);
        }
    }
}
