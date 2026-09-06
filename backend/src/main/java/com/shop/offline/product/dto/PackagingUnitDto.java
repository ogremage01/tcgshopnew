package com.shop.offline.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagingUnitDto {
    private Long id;
    private Long pieceId;
    private String pieceTitle;
    private String pieceProductId;
    private Long packagingId;
    private String packagingTitle;
    private String packagingProductId;
    private Long unitCount;
}
