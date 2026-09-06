package com.shop.scheduler.metadata.dto;

import com.shop.product.metadata.entity.TcgPProductLine;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductLineSyncDto {

    // 게임 라인 동기화 정보 Dto

    private Long productLineId;
    private String productLineName;
    private String productLineUrlName;
    private Boolean isDirect;

    public TcgPProductLine toEntity(Long id) {
        return TcgPProductLine.builder()
                .id(id)
                .productLineId(productLineId)
                .productLineName(productLineName)
                .productLineUrlName(productLineUrlName)
                .isDirect(isDirect)
                .build();
    }
}
