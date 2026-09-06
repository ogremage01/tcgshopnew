package com.shop.scheduler.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetNameProductTypePairDto {

    // 세트 이름과 상품 타입 페어 정보 Dto

    private Long productLineId;
    private Long setNameId;
    private Long productTypeId;
}
