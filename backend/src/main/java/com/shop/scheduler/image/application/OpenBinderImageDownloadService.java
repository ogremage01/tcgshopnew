package com.shop.scheduler.image.application;

public interface OpenBinderImageDownloadService {

    boolean downloadOpenBinderImages();

    void downloadOpenBinderImagesAsync();

    boolean isDownloadRunning();
}
