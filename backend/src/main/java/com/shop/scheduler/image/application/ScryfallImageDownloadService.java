package com.shop.scheduler.image.application;

public interface ScryfallImageDownloadService {

    boolean downloadScryfallImages();

    void downloadScryfallImagesAsync();

    boolean isDownloadRunning();
}
