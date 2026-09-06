package com.shop.offline.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagingUnitRequest {
    /** OfflineProduct.id (낱개) */
    private Long pieceId;
    /** OfflineProduct.id (포장) */
    private Long packagingId;
    /** 포장 1개당 낱개 수 */
    private Long unitCount;
}
