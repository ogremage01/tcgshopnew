package com.shop.product.metadata.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.shop.product.metadata.repository.SetNameMapRepository;
import com.shop.product.metadata.entity.SetNameMap;

@Service
@RequiredArgsConstructor
public class SetNameMapServiceImpl implements SetNameMapService {
    private final SetNameMapRepository setNameNormalizeRepository;

    @Override
    public SetNameMap findByName(String name) {
        return setNameNormalizeRepository.findByName(name).orElse(null);
    }

    @Override
    public SetNameMap findByGameAndName(String game, String name) {
        return setNameNormalizeRepository.findByGameAndName(game, name).orElse(null);
    }
}
