package com.shop.product.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shop.product.dto.GameSalesInfoDto;
import com.shop.product.entity.card.GameSalesInfo;
import com.shop.product.repository.sales.GameSalesInfoRepository;

import lombok.RequiredArgsConstructor;

//메서드 하나밖에 없으므로 인터페이스 생성 안함
@Service
@RequiredArgsConstructor
public class GameSalesInfoService {

    private final GameSalesInfoRepository gameSalesInfoRepository;

    public GameSalesInfoDto getGameSalesInfo(String game) {
        GameSalesInfo gameSalesInfo = gameSalesInfoRepository.findByGame(game).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game sales info not found"));
        if (gameSalesInfo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game sales info not found");
        }
        return GameSalesInfoDto.builder()
            .company(gameSalesInfo.getCompany())
            .brand(gameSalesInfo.getBrand())
            .origin(gameSalesInfo.getOrigin())
            .recommendedAge(gameSalesInfo.getRecommendedAge())
            .game(game)
            .build();
    }



}
