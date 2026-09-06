package com.shop.scheduler.price.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MtgSetInfoRawDto {

    // mtg-kr 세트 정보 데이터 원시 Dto

    @JsonProperty("set")
    private String setCode;
    @JsonProperty("name")
    private String name;
    @JsonProperty("name_k")
    private String nameK;
    @JsonProperty("release_date")
    private String releaseDate;
    @JsonProperty("type")
    private String type;

}
