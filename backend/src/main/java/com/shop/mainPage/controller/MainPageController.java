package com.shop.mainPage.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.service.BannerService;
import com.shop.config.mainHeader.dto.MainHeaderDto;
import com.shop.config.mainHeader.service.MainHeaderService;
import com.shop.config.mainPageContent.dto.MainPageContentDto;
import com.shop.config.mainPageContent.service.MainPageContentService;
import com.shop.mainPage.application.MainHeaderNavFacade;
import com.shop.mainPage.dto.HeaderNavGameSetsDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainPageController {
    private final BannerService bannerService;
    private final MainHeaderNavFacade mainHeaderNavFacade;
    private final MainPageContentService mainPageContentService;
    private final MainHeaderService mainHeaderService;

    @GetMapping("/banners")
    public ResponseEntity<List<BannerDto>> getBanners() {
        return ResponseEntity.ok(bannerService.findTargetByOrderByDisplayOrderAsc("home"));
    }

    @GetMapping("/header-nav/sets")
    public ResponseEntity<List<HeaderNavGameSetsDto>> getHeaderNavSets(
            @RequestParam(name = "limit", defaultValue = "6") int limit) {
        return ResponseEntity.ok(mainHeaderNavFacade.getLatestSetsByGame(limit));
    }

    @GetMapping("/header-nav/extras")
    public ResponseEntity<List<MainHeaderDto>> getHeaderNavExtras() {
        return ResponseEntity.ok(mainHeaderService.findActive());
    }

    @GetMapping("/content-blocks")
    public ResponseEntity<List<MainPageContentDto>> getContentBlocks() {
        return ResponseEntity.ok(mainPageContentService.findAll());
    }
}
