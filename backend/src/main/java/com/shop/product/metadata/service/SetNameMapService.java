package com.shop.product.metadata.service;

import com.shop.product.metadata.entity.SetNameMap;

public interface SetNameMapService {

    SetNameMap findByName(String name);

    SetNameMap findByGameAndName(String game, String name);

}
