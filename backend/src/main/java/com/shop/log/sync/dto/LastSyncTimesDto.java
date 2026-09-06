package com.shop.log.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 메타데이터/가격 동기화 대상별 최신 동기 로그 (설정 화면·last-time API 공통)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LastSyncTimesDto {

    private SyncLogDto lastPriceSyncTime;
    private SyncLogDto lastMetadataSyncTime;
    private SyncLogDto lastOpenBinderSyncTime;
    private SyncLogDto lastImageDownloadSyncTime;
    private SyncLogDto lastPriceLinkOverwriteSyncTime;
}
