package com.shop.scheduler.price.source.openbinder;

public interface OpenBinderSyncService {

    OpenBinderSyncResult syncAll();

    String syncMtgPrices();

    String syncFabPrices();

    String syncMtgSetInfo();

    String syncFabSetInfo();
}
