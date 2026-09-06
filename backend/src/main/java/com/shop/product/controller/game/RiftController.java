package com.shop.product.controller.game;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.service.BannerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rift")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:3000}", allowCredentials = "true")
@RequiredArgsConstructor
public class RiftController {
    private final BannerService bannerService;

    @GetMapping("/banners")
    public ResponseEntity<List<BannerDto>> getBanners() {
        return ResponseEntity.ok(bannerService.findTargetByOrderByDisplayOrderAsc("rift"));
    }

}
