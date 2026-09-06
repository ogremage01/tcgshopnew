package com.shop.scheduler.price.service.batch.deprecated;

import java.util.List;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import lombok.extern.slf4j.Slf4j;

/**
 * [DEPRECATED] 레거시 가격 배치 저장. 공개본에서는 구현을 포함하지 않습니다.
 */
@Deprecated
@Slf4j
public class PriceBatchSaverLegacy {

    @Deprecated
    public void saveTcgBatch(List<TcgPPrice> incoming) {
        log.warn("Portfolio snapshot: legacy price batch save is omitted.");
    }

    @Deprecated
    public void saveMtgBatch(List<MtgPrice> incoming) {
        log.warn("Portfolio snapshot: legacy price batch save is omitted.");
    }

    @Deprecated
    public void saveFabBatch(List<FabPrice> incoming) {
        log.warn("Portfolio snapshot: legacy price batch save is omitted.");
    }
}
