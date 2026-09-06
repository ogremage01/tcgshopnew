package com.shop.admin.product.controller;

import com.shop.product.dto.sealed.AddSealedProductDto;
import com.shop.product.dto.sealed.SealedProductDto;
import com.shop.product.dto.sealed.UpdateSealedProductDto;
import com.shop.product.dto.sealed.SealedProductGameFacetDto;
import com.shop.product.service.sealedProduct.SealedProductService;
import lombok.RequiredArgsConstructor;
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

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/product/sealed-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSealedProductController {

    private final SealedProductService sealedProductService;

    @PostMapping
    public ResponseEntity<String> createSealedProduct(@ModelAttribute AddSealedProductDto dto) {
        try {
            sealedProductService.createSealedProduct(dto);
            return ResponseEntity.ok("SealedProduct creation successful");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SealedProduct creation failed");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SealedProductDto> getSealedProductById(@PathVariable Long id) {
        return ResponseEntity.ok(sealedProductService.getSealedProductById(id));
    }

    @GetMapping
    public ResponseEntity<Page<SealedProductDto>> getSealedProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String setCode,
            @RequestParam(required = false) String language,
            Pageable pageable) {
        return ResponseEntity.ok(sealedProductService.getSealedProductList(keyword, game, setCode, language, pageable));
    }

    @GetMapping("/facets")
    public ResponseEntity<List<SealedProductGameFacetDto>> getFacets() {
        return ResponseEntity.ok(sealedProductService.getFacets());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSealedProduct(
            @PathVariable Long id,
            @ModelAttribute UpdateSealedProductDto dto) throws IOException {
        sealedProductService.updateSealedProduct(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSealedProduct(@PathVariable Long id) {
        sealedProductService.deleteSealedProduct(id);
        return ResponseEntity.ok().build();
    }
}
