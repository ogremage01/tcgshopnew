package com.shop.admin.product.controller;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.product.dto.manual.AddManualProductDto;
import com.shop.product.dto.manual.ManualProductDto;
import com.shop.product.dto.manual.UpdateManualProductDto;
import com.shop.product.service.manualProduct.ManualProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/product/manual-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminManualProductController {

    private final ManualProductService manualProductService;

    @PostMapping
    public ResponseEntity<String> createManualProduct(@ModelAttribute AddManualProductDto addManualProductDto) throws IOException {
        try {
            manualProductService.createManualProduct(addManualProductDto);
            return ResponseEntity.ok("ManualProduct creation successful");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ManualProduct creation failed");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateManualProduct(
            @PathVariable Long id,
            @ModelAttribute UpdateManualProductDto request) throws IOException {
        manualProductService.updateManualProduct(id, request);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManualProduct(@PathVariable Long id) {
        manualProductService.deleteManualProduct(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManualProductDto> getManualProduct(@PathVariable Long id) {
        return ResponseEntity.ok(manualProductService.getManualProductById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ManualProductDto>> getManualProductList(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(manualProductService.getManualProductList(keyword.trim(), pageable));
        }
        return ResponseEntity.ok(manualProductService.getManualProductList(pageable));
    }
}
