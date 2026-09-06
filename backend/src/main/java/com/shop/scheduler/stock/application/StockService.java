package com.shop.scheduler.stock.application;

public interface StockService {

    void syncStock();

    /**
     * 재고 충전을 백그라운드에서 시작합니다. 이미 실행 중이면 false를 반환합니다.
     *
     * @param logSource sync log source (예: scheduler.stock, admin.stock)
     */
    boolean startSyncStock(String logSource);

    /**
     * 재고 충전을 실행하고 동기화 로그를 남깁니다.
     *
     * @param logSource sync log source (예: scheduler.stock, admin.stock)
     */
    void runStockSyncJob(String logSource);
}
