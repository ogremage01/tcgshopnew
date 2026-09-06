package com.shop.scheduler.price.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FabSetInfoRawDto {

    // fab 세트 정보 데이터 원시 Dto

    @JsonProperty("set")
    private String setCode;
    @JsonProperty("name")
    private String name;
    @JsonProperty("porder")
    private Long porder;
    @JsonProperty("category")
    private String category;

}
