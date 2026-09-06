package com.shop.mainPage.application;

import java.util.List;

import com.shop.mainPage.dto.HeaderNavGameSetsDto;

public interface MainHeaderNavFacade {
    List<HeaderNavGameSetsDto> getLatestSetsByGame(int limit);
}