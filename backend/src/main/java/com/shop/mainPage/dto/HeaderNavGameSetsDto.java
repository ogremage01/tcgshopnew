package com.shop.mainPage.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HeaderNavGameSetsDto {
    private final String gameCode;
    private final List<HeaderNavSetDto> sets;
}
