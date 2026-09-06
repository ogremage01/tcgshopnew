package com.shop.card.metadata.dto;

import com.shop.card.metadata.entity.FabSetInfo;
import com.shop.card.metadata.support.FabSetCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FabSetInfoDto {

    private String setCode;
    private String name;
    private Long porder;

    public static FabSetInfoDto from(FabSetInfo entity) {
        return new FabSetInfoDto(FabSetCode.toDisplayCode(entity.getSetCode()), entity.getName(), entity.getPorder());
    }
}
