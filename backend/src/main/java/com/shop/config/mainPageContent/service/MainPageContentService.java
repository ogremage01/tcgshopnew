package com.shop.config.mainPageContent.service;

import java.util.List;

import com.shop.config.mainPageContent.dto.ChangeMainPageContentOrderDto;
import com.shop.config.mainPageContent.dto.MainPageContentDto;
import com.shop.config.mainPageContent.dto.SaveMainPageContentDto;

public interface MainPageContentService {
    List<MainPageContentDto> findAll();
    void add(SaveMainPageContentDto dto);
    void update(Long id, SaveMainPageContentDto dto);
    void delete(Long id);
    void changeOrder(ChangeMainPageContentOrderDto dto);
}
