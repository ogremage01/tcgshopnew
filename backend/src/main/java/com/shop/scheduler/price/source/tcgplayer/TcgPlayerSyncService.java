package com.shop.scheduler.price.source.tcgplayer;

import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;

import java.util.List;
import java.util.Map;

public interface TcgPlayerSyncService {

    Map<Long, List<SetNameProductTypePairDto>> buildSyncPlan(List<SetNameProductTypePairDto> flatPairs);

    TcgPlayerSyncResult syncAllProductLines(Map<Long, List<SetNameProductTypePairDto>> syncPlanByProductLineId);

    TcgPlayerSyncResult syncProductLine(Long productLineId);
}
