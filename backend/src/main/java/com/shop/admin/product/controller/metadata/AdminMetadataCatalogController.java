package com.shop.admin.product.controller.metadata;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.admin.product.service.metadata.AdminProductMetadataService;
import com.shop.card.metadata.dto.FabSetInfoDto;
import com.shop.card.metadata.repository.FabSetInfoRepository;
import com.shop.product.dto.card.management.TcgPSyncGameDto;
import com.shop.product.metadata.dto.TcgPProductLineDto;
import com.shop.product.metadata.dto.TcgPSetInfoDto;
import com.shop.product.metadata.service.TcgPSetNameService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/product/metadata")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetadataCatalogController {

    private final AdminProductMetadataService adminProductMetadataService;
    private final TcgPSetNameService tcgPSetNameService;
    private final FabSetInfoRepository fabSetInfoRepository;

    // 상품 카탈로그 관련 기능입니다. (카탈로그 조회/동기화 대상 게임 조회)

    @GetMapping("/list")
    public ResponseEntity<List<TcgPProductLineDto>> getProductLineList() {
        List<TcgPProductLineDto> productLineList = adminProductMetadataService.getProductLineList();
        return ResponseEntity.ok(productLineList);
    }

    @GetMapping("/list/{productLineId}")
    public ResponseEntity<List<TcgPSetInfoDto>> getSetNameList(@PathVariable("productLineId") Long productLineId) {
        List<TcgPSetInfoDto> setNameList = tcgPSetNameService
                .findSetInfoListByProductLineIdOrderByReleaseDateDesc(productLineId);
        return ResponseEntity.ok(setNameList);
    }

    @GetMapping("/fab/sets")
    public ResponseEntity<List<FabSetInfoDto>> getFabSetInfoList() {
        return ResponseEntity.ok(fabSetInfoRepository.findAllByOrderByPorderAsc().stream()
                .map(FabSetInfoDto::from)
                .toList());
    }

    @GetMapping("/sync/game/list")
    public ResponseEntity<List<TcgPSyncGameDto>> getSyncGameList() {
        List<TcgPSyncGameDto> syncGameList = adminProductMetadataService.getSyncGameList();
        return ResponseEntity.ok(syncGameList);
    }
}
