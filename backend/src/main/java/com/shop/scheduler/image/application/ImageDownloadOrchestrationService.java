package com.shop.scheduler.image.application;

/**
 * OpenBinder 이미지 다운로드 후 Scryfall 폴백을 순차로 실행한다.
 */
public interface ImageDownloadOrchestrationService {

    boolean downloadOpenBinderThenScryfall();

    void downloadOpenBinderThenScryfallAsync();

    boolean isDownloadRunning();
}
