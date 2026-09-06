package com.shop.product.service;

import java.util.List;

import com.shop.product.dto.card.CardProductLanguageDto;

public interface CardProductLanguageService {

    List<CardProductLanguageDto> findActiveLanguages();
}
