package com.shop.config.mainHeader.service;

import java.util.List;

import com.shop.config.mainHeader.dto.ChangeMainHeaderOrderDto;
import com.shop.config.mainHeader.dto.MainHeaderDto;
import com.shop.config.mainHeader.dto.SaveMainHeaderDto;

public interface MainHeaderService {
    List<MainHeaderDto> findAll();

    List<MainHeaderDto> findActive();

    void add(SaveMainHeaderDto dto);

    void update(Long id, SaveMainHeaderDto dto);

    void delete(Long id);

    void changeOrder(ChangeMainHeaderOrderDto dto);
}
