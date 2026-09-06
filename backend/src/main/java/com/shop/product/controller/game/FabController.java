package com.shop.product.controller.game;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.card.metadata.dto.FabSetInfoDto;
import com.shop.card.metadata.repository.FabSetInfoRepository;
import com.shop.card.metadata.support.FabSetCode;
import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.dto.SetBannerDto;
import com.shop.config.banner.service.BannerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fab")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:3000}", allowCredentials = "true")
@RequiredArgsConstructor
public class FabController {
    private final BannerService bannerService;
    private final FabSetInfoRepository fabSetInfoRepository;

    @GetMapping("/banners")
    public ResponseEntity<List<BannerDto>> getBanners() {
        return ResponseEntity.ok(bannerService.findTargetByOrderByDisplayOrderAsc("fab"));
    }

    @GetMapping("/sets")
    public ResponseEntity<List<FabSetInfoDto>> getFabSetInfoList() {
        return ResponseEntity.ok(fabSetInfoRepository.findAllByOrderByPorderAsc().stream()
                .map(FabSetInfoDto::from)
                .toList());
    }

    @GetMapping("/sets/{setCode}")
    public ResponseEntity<FabSetInfoDto> getFabSetInfo(@PathVariable String setCode) {
        return fabSetInfoRepository.findBySetCodeIn(List.of(setCode, FabSetCode.toSourceCode(setCode))).stream()
                .findFirst()
                .map(FabSetInfoDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sets/{setCode}/banner")
    public ResponseEntity<SetBannerDto> getSetBanner(@PathVariable String setCode) {
        return bannerService.findActiveSetBanner("fab", setCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
