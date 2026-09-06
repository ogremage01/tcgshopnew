package com.shop.admin.product.controller.metadata;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.admin.product.dto.metadata.ProductConfigBootstrapDto;
import com.shop.admin.product.service.metadata.AdminProductConfigBootstrapService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/product/metadata")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductConfigBootstrapController {

    private final AdminProductConfigBootstrapService adminProductConfigBootstrapService;

    /**
     * 상품 설정 화면 초기 로딩용 (게임·동기 시각·저장소·최소/등급 가격) 집계
     */
    @GetMapping("/config/bootstrap")
    public ResponseEntity<ProductConfigBootstrapDto> getConfigBootstrap() {
        return ResponseEntity.ok(adminProductConfigBootstrapService.getBootstrap());
    }
}
