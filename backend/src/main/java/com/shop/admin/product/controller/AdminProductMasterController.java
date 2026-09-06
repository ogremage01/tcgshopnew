package com.shop.admin.product.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.product.dto.ProductIpCreateRequestDto;
import com.shop.product.dto.ProductIpDto;
import com.shop.product.dto.manual.ProductCategoryCreateRequestDto;
import com.shop.product.dto.manual.ProductCategoryDto;
import com.shop.product.service.ProductMasterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/product")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductMasterController {

    private final ProductMasterService productMasterService;

    // ------------------------------------------------------
    // 상품 카테고리 (Manual)
    // ------------------------------------------------------

    @PostMapping("/product-categories")
    public ResponseEntity<Void> createCategory(@RequestBody ProductCategoryCreateRequestDto request) {
        productMasterService.createCategory(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/product-categories/{id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        productMasterService.updateCategory(id, body.get("nameEn"), body.get("nameKo"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/product-categories/delete/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        productMasterService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/product-categories")
    public ResponseEntity<List<ProductCategoryDto>> getCategoryList() {
        return ResponseEntity.ok(productMasterService.getCategoryList());
    }

    @GetMapping("/product-categories-by-page")
    public ResponseEntity<Page<ProductCategoryDto>> getCategoryListByPage(Pageable pageable) {
        return ResponseEntity.ok(productMasterService.getCategoryListByPage(pageable));
    }

    // ------------------------------------------------------
    // 제품 IP (Manual / Sealed 공용)
    // ------------------------------------------------------

    @PostMapping("/product-ips")
    public ResponseEntity<Void> createProductIp(@RequestBody ProductIpCreateRequestDto request) {
        productMasterService.createProductIp(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/product-ips/{id}")
    public ResponseEntity<Void> updateProductIp(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        productMasterService.updateProductIp(id, body.get("nameEn"), body.get("nameKo"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/product-ips/delete/{id}")
    public ResponseEntity<Void> deleteProductIp(@PathVariable Long id) {
        productMasterService.deleteProductIp(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/product-ips")
    public ResponseEntity<List<ProductIpDto>> getProductIpList() {
        return ResponseEntity.ok(productMasterService.getProductIpList());
    }

    @GetMapping("/product-ips-by-page")
    public ResponseEntity<Page<ProductIpDto>> getProductIpListByPage(Pageable pageable) {
        return ResponseEntity.ok(productMasterService.getProductIpListByPage(pageable));
    }
}
