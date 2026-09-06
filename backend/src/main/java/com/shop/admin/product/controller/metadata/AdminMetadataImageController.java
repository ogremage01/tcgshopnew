package com.shop.admin.product.controller.metadata;

import lombok.RequiredArgsConstructor;

import com.shop.scheduler.image.application.ImageDownloadOrchestrationService;
import com.shop.scheduler.image.application.ScryfallImageDownloadService;
import com.shop.scheduler.image.application.TcgPImageDownloadService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/product/metadata")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetadataImageController {
    // 이미지 다운로드 관련 기능입니다. (수동 다운로드 시작)

    private final TcgPImageDownloadService tcgPImageDownloadService;
    private final ImageDownloadOrchestrationService imageDownloadOrchestrationService;
    private final ScryfallImageDownloadService scryfallImageDownloadService;

    /**
     * 이미지 다운로드를 시작합니다.
     *
     * @return 다운로드 시작 결과 메시지
     */
    @PostMapping("/tcg-p-images/download")
    public ResponseEntity<String> tcgPImagesDownloadManualStart() {
        if (tcgPImageDownloadService.isDownloadRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미지 다운로드가 이미 진행 중입니다.");
        }
        tcgPImageDownloadService.downloadTcgPImagesAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("이미지 다운로드를 백그라운드에서 시작했습니다.");
    }

    @PostMapping("/openbinder-images/download")
    public ResponseEntity<String> openBinderImagesDownloadManualStart() {
        if (imageDownloadOrchestrationService.isDownloadRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("OpenBinder image download is already running.");
        }
        imageDownloadOrchestrationService.downloadOpenBinderThenScryfallAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("OpenBinder image download started in background. Scryfall fallback will run after it finishes.");
    }

    @PostMapping("/scryfall-images/download")
    public ResponseEntity<String> scryfallImagesDownloadManualStart() {
        if (imageDownloadOrchestrationService.isDownloadRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Scryfall image download is already running.");
        }
        scryfallImageDownloadService.downloadScryfallImagesAsync();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Scryfall image download started in background.");
    }

}
