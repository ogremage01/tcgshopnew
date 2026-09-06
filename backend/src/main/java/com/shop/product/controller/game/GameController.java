package com.shop.product.controller.game;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.config.banner.dto.SetBannerDto;
import com.shop.config.banner.service.BannerService;
import com.shop.product.enums.GameEnum;
import com.shop.product.metadata.dto.TcgPSetInfoDto;
import com.shop.product.metadata.service.TcgPSetNameService;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:3000}", allowCredentials = "true")
@RequiredArgsConstructor
public class GameController {
    private final TcgPSetNameService tcgPSetNameService;
    private final BannerService bannerService;

    //게임 세트 목록
    @GetMapping("/sets/{productLineId}")
    public ResponseEntity<List<TcgPSetInfoDto>> getTcgPSetInfoList(@PathVariable Long productLineId) {
        List<TcgPSetInfoDto> tcgPSetInfoList = tcgPSetNameService.findSetInfoListByProductLineIdOrderByReleaseDateDesc(productLineId);
        return ResponseEntity.ok(tcgPSetInfoList);
    }

    //개별 세트 제품 정보
    @GetMapping("/{game}/sets/{setName}")
    public ResponseEntity<TcgPSetInfoDto> getTcgPSetInfo(@PathVariable String game, @PathVariable String setName) {

        TcgPSetInfoDto tcgPSetInfo = tcgPSetNameService.findSetInfoByGameAndSetName(GameEnum.fromCode(game).getProductLineName(), setName);
        return ResponseEntity.ok(tcgPSetInfo);
    }

    @GetMapping("/{game}/sets/{setName}/banner")
    public ResponseEntity<SetBannerDto> getSetBanner(@PathVariable String game, @PathVariable String setName) {
        return bannerService.findActiveSetBanner(game, setName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
