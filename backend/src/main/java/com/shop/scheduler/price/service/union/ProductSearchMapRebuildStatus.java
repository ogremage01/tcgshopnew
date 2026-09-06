package com.shop.scheduler.price.service.union;

public record ProductSearchMapRebuildStatus(
        boolean running,
        String phase,
        int currentPage,
        long processedRows,
        String lastMessage) {
}
