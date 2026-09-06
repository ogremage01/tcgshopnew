package com.shop.scheduler.price.application;

import java.util.List;
import java.util.Map;

import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;

public interface PriceService {

    List<SetNameProductTypePairDto> getTargetSetNameAndTypeIds();

    Map<Long, List<SetNameProductTypePairDto>> getTargetSyncPlan();

    boolean startSyncPrices();

    boolean startSyncPricesWithPriority();

    boolean isSyncRunning();

    String syncPrices(List<SetNameProductTypePairDto> flatPairs);

    String syncPricesForProductLine(Long productLineId);

    String syncOpenBinderPrices();

    String syncOpenBinderPricesWithPriority();

    boolean startSyncOpenBinderPrices();

    boolean isOpenBinderSyncRunning();

    String syncMtgPrices();

    String syncFabPrices();
}
