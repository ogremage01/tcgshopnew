package com.shop.card.metadata.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MtgSetInfoDto {

    // MtgSetInfo Dto

    private String setCode;
    private String name;
    private String nameK;
    private String type;
    private LocalDateTime releaseDate;

}
