package com.shop.product.metadata.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.product.metadata.dto.TcgPSetInfoDto;
import com.shop.product.metadata.repository.TcgPSetNameRepository;

import lombok.RequiredArgsConstructor;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class TcgPSetNameServiceImpl implements TcgPSetNameService {

    private final TcgPSetNameRepository tcgPSetNameRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TcgPSetInfoDto> findSetInfoListByProductLineIdOrderByReleaseDateDesc(Long productLineId) {
        return tcgPSetNameRepository.findSetInfoListByProductLineIdOrderByReleaseDateDesc(productLineId);
    }
    @Override
    @Transactional(readOnly = true)
    public TcgPSetInfoDto findSetInfoByGameAndSetName(String game, String setName) {
        return tcgPSetNameRepository.findSetInfoByGameAndSetName(game, setName).orElse(null);
    }
    @Override
    @Transactional(readOnly = true)
    public Optional<TcgPSetInfoDto> findSetInfoByCategoryIdAndSetName(Long categoryId, String setName) {
        return tcgPSetNameRepository.findSetInfoByCategoryIdAndSetName(categoryId, setName);
    }

}
