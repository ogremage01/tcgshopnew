package com.shop.scheduler.price.source.tcgplayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 카탈로그 가격 동기화 파이프라인은 포함하지 않습니다.
 */
@Slf4j
@Service
public class TcgPlayerSyncServiceImpl implements TcgPlayerSyncService {

    private static final String OMITTED = "omitted in public snapshot";

    @Override
    public Map<Long, List<SetNameProductTypePairDto>> buildSyncPlan(List<SetNameProductTypePairDto> flatPairs) {
        Map<Long, List<SetNameProductTypePairDto>> syncPlanByProductLineId = new LinkedHashMap<>();
        if (flatPairs == null) {
            return syncPlanByProductLineId;
        }
        for (SetNameProductTypePairDto setTypePair : flatPairs) {
            syncPlanByProductLineId
                    .computeIfAbsent(setTypePair.getProductLineId(), id -> new ArrayList<>())
                    .add(setTypePair);
        }
        return syncPlanByProductLineId;
    }

    @Override
    public TcgPlayerSyncResult syncAllProductLines(Map<Long, List<SetNameProductTypePairDto>> syncPlanByProductLineId) {
        log.warn("Portfolio snapshot: catalog price sync is omitted.");
        return new TcgPlayerSyncResult("failure", OMITTED);
    }

    @Override
    public TcgPlayerSyncResult syncProductLine(Long productLineId) {
        log.warn("Portfolio snapshot: catalog price sync is omitted.");
        return new TcgPlayerSyncResult("failure", OMITTED);
    }
}
