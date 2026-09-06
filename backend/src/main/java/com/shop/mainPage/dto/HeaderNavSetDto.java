package com.shop.mainPage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HeaderNavSetDto {
    private final String setCode;
    private final String setName;
    /** TcgP 게임 URL 세그먼트. MTG는 null( setCode 사용). */
    private final String urlName;
}
