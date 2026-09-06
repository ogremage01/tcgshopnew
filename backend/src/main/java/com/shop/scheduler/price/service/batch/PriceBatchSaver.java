package com.shop.scheduler.price.service.batch;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 가격 UPSERT SQL과 중복 키 처리는 포함하지 않습니다.
 */
@Slf4j
@Service
public class PriceBatchSaver {

    public void saveTcgBatch(List<TcgPPrice> incoming) {
        omitted();
    }

    public void saveMtgBatch(List<MtgPrice> incoming) {
        omitted();
    }

    public void saveFabBatch(List<FabPrice> incoming) {
        omitted();
    }

    private static void omitted() {
        log.warn("Portfolio snapshot: price batch upsert is omitted.");
    }
}
