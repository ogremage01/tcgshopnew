package com.shop.product.metadata.service;

import java.util.List;
import java.util.Optional;

import com.shop.product.metadata.dto.TcgPSetInfoDto;

public interface TcgPSetNameService {
    List<TcgPSetInfoDto> findSetInfoListByProductLineIdOrderByReleaseDateDesc(Long productLineId);
    TcgPSetInfoDto findSetInfoByGameAndSetName(String game, String setName);

    Optional<TcgPSetInfoDto> findSetInfoByCategoryIdAndSetName(Long categoryId, String setName);
}
