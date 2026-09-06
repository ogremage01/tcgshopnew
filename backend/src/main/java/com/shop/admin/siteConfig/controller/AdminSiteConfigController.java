package com.shop.admin.siteConfig.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shop.common.fileUpload.service.FileUploadService;
import com.shop.config.banner.dto.AddBannerDto;
import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.dto.ChangeBannerOrderDto;
import com.shop.config.banner.dto.SetBannerDto;
import com.shop.config.banner.dto.UpdateSetBannerDto;
import com.shop.config.banner.service.BannerService;
import com.shop.config.mainHeader.dto.ChangeMainHeaderOrderDto;
import com.shop.config.mainHeader.dto.MainHeaderDto;
import com.shop.config.mainHeader.dto.SaveMainHeaderDto;
import com.shop.config.mainHeader.service.MainHeaderService;
import com.shop.config.mainPageContent.dto.ChangeMainPageContentOrderDto;
import com.shop.config.mainPageContent.dto.MainPageContentDto;
import com.shop.config.mainPageContent.dto.SaveMainPageContentDto;
import com.shop.config.mainPageContent.service.MainPageContentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/site-setting")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSiteConfigController {
    private final BannerService bannerService;
    private final MainPageContentService mainPageContentService;
    private final MainHeaderService mainHeaderService;
    private final FileUploadService fileUploadService;


    @GetMapping("/banners/list/{target}")
    public ResponseEntity<List<BannerDto>> getMainBannerList(@PathVariable String target) {
        return ResponseEntity.ok(bannerService.findTargetByOrderByDisplayOrderAsc(target));
    }

    @PostMapping("/banners/add/{target}")
    public ResponseEntity<String> addBanner(@PathVariable String target, @ModelAttribute AddBannerDto dto) throws IOException {
        System.out.println("dto.getImageFile() = " + dto.getImageFile());
        try {
            bannerService.addBanner(target, dto);
            return ResponseEntity.ok("배너 추가 성공");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("배너 추가 실패: " + e.getMessage());
        }
    }

    @PutMapping("/banners/order/{target}")
    public ResponseEntity<String> changeBannerOrder(@PathVariable String target, @RequestBody ChangeBannerOrderDto dto) {
        bannerService.changeBannerOrder(target, dto);
        return ResponseEntity.ok("배너 순서 변경 성공");
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<String> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok("배너 삭제 성공");
    }

    // --- 세트 배너 (수정만) ---

    @GetMapping("/set-banners/list")
    public ResponseEntity<List<SetBannerDto>> getSetBannerList() {
        return ResponseEntity.ok(bannerService.findAllSetBanners());
    }

    @PutMapping("/set-banners/{game}/{bannerId}")
    public ResponseEntity<String> updateSetBanner(
            @PathVariable String game,
            @PathVariable String bannerId,
            @ModelAttribute UpdateSetBannerDto dto) throws IOException {
        try {
            bannerService.updateSetBanner(game, bannerId, dto);
            return ResponseEntity.ok("세트 배너 수정 성공");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("세트 배너 수정 실패: " + e.getMessage());
        }
    }

    // --- 메인 페이지 콘텐츠 ---

    @GetMapping("/main-page-content/list")
    public ResponseEntity<List<MainPageContentDto>> getMainPageContentList() {
        return ResponseEntity.ok(mainPageContentService.findAll());
    }

    @PostMapping("/main-page-content/add")
    public ResponseEntity<String> addMainPageContent(@RequestBody SaveMainPageContentDto dto) {
        mainPageContentService.add(dto);
        return ResponseEntity.ok("콘텐츠 추가 성공");
    }

    @PutMapping("/main-page-content/{id}")
    public ResponseEntity<String> updateMainPageContent(@PathVariable Long id, @RequestBody SaveMainPageContentDto dto) {
        mainPageContentService.update(id, dto);
        return ResponseEntity.ok("콘텐츠 수정 성공");
    }

    @PutMapping("/main-page-content/order")
    public ResponseEntity<String> changeMainPageContentOrder(@RequestBody ChangeMainPageContentOrderDto dto) {
        mainPageContentService.changeOrder(dto);
        return ResponseEntity.ok("콘텐츠 순서 변경 성공");
    }

    @DeleteMapping("/main-page-content/{id}")
    public ResponseEntity<String> deleteMainPageContent(@PathVariable Long id) {
        mainPageContentService.delete(id);
        return ResponseEntity.ok("콘텐츠 삭제 성공");
    }

    // --- 헤더 메뉴 (단일 항목 extras) ---

    @GetMapping("/main-header/list")
    public ResponseEntity<List<MainHeaderDto>> getMainHeaderList() {
        return ResponseEntity.ok(mainHeaderService.findAll());
    }

    @PostMapping("/main-header/add")
    public ResponseEntity<String> addMainHeader(@RequestBody SaveMainHeaderDto dto) {
        mainHeaderService.add(dto);
        return ResponseEntity.ok("헤더 메뉴 추가 성공");
    }

    @PutMapping("/main-header/{id}")
    public ResponseEntity<String> updateMainHeader(@PathVariable Long id, @RequestBody SaveMainHeaderDto dto) {
        mainHeaderService.update(id, dto);
        return ResponseEntity.ok("헤더 메뉴 수정 성공");
    }

    @PutMapping("/main-header/order")
    public ResponseEntity<String> changeMainHeaderOrder(@RequestBody ChangeMainHeaderOrderDto dto) {
        mainHeaderService.changeOrder(dto);
        return ResponseEntity.ok("헤더 메뉴 순서 변경 성공");
    }

    @DeleteMapping("/main-header/{id}")
    public ResponseEntity<String> deleteMainHeader(@PathVariable Long id) {
        mainHeaderService.delete(id);
        return ResponseEntity.ok("헤더 메뉴 삭제 성공");
    }

    // --- EditorJS 이미지 업로드 ---

    @PostMapping("/upload/main-image")
    public ResponseEntity<Map<String, Object>> uploadMainImage(@RequestParam("image") MultipartFile file) {
        try {
            String url = fileUploadService.uploadFile(file, "main");
            return ResponseEntity.ok(Map.of(
                    "success", 1,
                    "file", Map.of("url", "/" + url)
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", 0, "message", e.getMessage()));
        }
    }
}
