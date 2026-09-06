package com.shop.scheduler.metadata.application;

import java.util.List;

public interface MetadataService {
    record SyncResult(String result, String message) {
    }

    public SyncResult syncProductLines(String url);

    public SyncResult syncSetNames(List<Long> categoryIds, String url);

    public SyncResult syncProductTypes(List<Long> productLineIds, String url);

    public Boolean syncAll();

    public Boolean syncAllWithPriority();
}
