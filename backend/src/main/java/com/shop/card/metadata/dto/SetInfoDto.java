package com.shop.card.metadata.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SetInfoDto {

    // 세트 정보 Dto

    private String setCode;
    private String SetName;
    private LocalDateTime releaseDate;

}
