package com.shop.product.controller.game;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.card.metadata.dto.MtgSetInfoDto;
import com.shop.card.metadata.service.MtgInfoService;
import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.dto.SetBannerDto;
import com.shop.config.banner.service.BannerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mtg")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:3000}", allowCredentials = "true")
@RequiredArgsConstructor
public class MtgController {

    private final MtgInfoService mtgInfoService;
    private final BannerService bannerService;

    //배너 목록
    @GetMapping("/banners")
    public ResponseEntity<List<BannerDto>> getBanners() {
        return ResponseEntity.ok(bannerService.findTargetByOrderByDisplayOrderAsc("mtg"));
    }

    //세트 목록
    @GetMapping("/sets")
    public ResponseEntity<List<MtgSetInfoDto>> getMtgSetInfoList() {
        List<MtgSetInfoDto> mtgSetInfoList = mtgInfoService.getMtgSetInfoList();
        return ResponseEntity.ok(mtgSetInfoList);
    }
    
    //개별 세트 제품 정보
    @GetMapping("/sets/{setCode}")
    public ResponseEntity<MtgSetInfoDto> getMtgSetInfo(@PathVariable String setCode) {
        MtgSetInfoDto mtgSetInfo = mtgInfoService.getMtgSetInfo(setCode);
        return ResponseEntity.ok(mtgSetInfo);
    }

    @GetMapping("/sets/{setCode}/banner")
    public ResponseEntity<SetBannerDto> getSetBanner(@PathVariable String setCode) {
        return bannerService.findActiveSetBanner("mtg", setCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
