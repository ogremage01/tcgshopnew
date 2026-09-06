package com.shop.admin.product.controller.metadata;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.admin.product.service.metadata.AdminProductMetadataService;
import com.shop.product.metadata.dto.StorageDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/product/metadata")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetadataStorageController {

    private final AdminProductMetadataService adminProductMetadataService;

    // 상품 보관소 관련 기능입니다. (조회/등록/수정)

    /**
     * 상품 보관소 목록을 조회합니다.
     *
     * @return 상품 보관소 목록
     */
    @GetMapping("/storage/list")
    public ResponseEntity<List<StorageDto>> getStorageList() {
        List<StorageDto> storageList = adminProductMetadataService.getStorageList();
        return ResponseEntity.ok(storageList);
    }

    /**
     * 상품 보관소를 등록합니다.
     *
     * @param storageDto 상품 보관소 등록 정보
     * @return 등록 결과 메시지
     */
    @PostMapping("/storage")
    public ResponseEntity<String> setStorage(@RequestBody StorageDto storageDto) {
        adminProductMetadataService.setStorage(storageDto);
        return ResponseEntity.ok("Storage set successfully.");
    }

    /**
     * 상품 보관소를 수정합니다.
     *
     * @param storageDto 상품 보관소 수정 정보
     * @return 수정 결과 메시지
     */
    @PutMapping("/storage")
    public ResponseEntity<String> updateStorage(@RequestBody StorageDto storageDto) {
        adminProductMetadataService.updateStorage(storageDto);
        return ResponseEntity.ok("Storage set successfully.");
    }

    /**
     * 상품 보관소를 삭제합니다.
     * 보관중인 상품은 전부 삭제처리됩니다.
     *
     * @param storageDto 상품 보관소 수정 정보
     * @return 수정 결과 메시지
     */
    @PutMapping("/storage/delete")
    public ResponseEntity<String> deleteStorage(@RequestBody StorageDto storageDto) {
        adminProductMetadataService.deleteStorage(storageDto);
        return ResponseEntity.ok("Storage deleted successfully.");
    }
}
