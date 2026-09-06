package com.shop.offline.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 현재 입고/출고 기준 묶음 전환 + 온라인 재고 동기화 결과.
 * (출고 절대값 재집계는 포함하지 않음)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineStockSyncResultDto {

    /** 처리한 Offline_products 수 */
    private int processedProductCount;
}
