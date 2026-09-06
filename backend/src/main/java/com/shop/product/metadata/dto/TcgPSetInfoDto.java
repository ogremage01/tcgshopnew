package com.shop.product.metadata.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TcgPSetInfoDto {

    private String setCode;
    private String setName;
    private String urlName;
    private LocalDateTime releaseDate;
}
