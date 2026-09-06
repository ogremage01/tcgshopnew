package com.shop.scheduler.price.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceRawDto {

    // mtg-kr 가격 데이터 원시 Dto

    private String set;
    @JsonProperty("setname")
    private String setName;
    private String code;
    private String rarity;
    private String name;
    @JsonProperty("name_k")
    private String nameK;
    private String finishes;
    private Map<String, Object> price;

}
