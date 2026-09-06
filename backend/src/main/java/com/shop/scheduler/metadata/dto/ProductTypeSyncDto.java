package com.shop.scheduler.metadata.dto;

import com.shop.product.metadata.entity.TcgPProductType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductTypeSyncDto {

    // 상품 타입 동기화 정보 Dto

    private Long productTypeId;
    private String productName;

    public TcgPProductType toEntity(Long id, Long productLineId) {
        return TcgPProductType.builder()
                .id(id)
                .productTypeId(productTypeId)
                .productName(productName)
                .productLineId(productLineId)
                .build();
    }
}
