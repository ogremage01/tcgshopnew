package com.shop.scheduler.image.application;

public interface TcgPImageDownloadService {
    public boolean downloadTcgPImages();

    public void downloadTcgPImagesAsync();

    public boolean isDownloadRunning();
}
