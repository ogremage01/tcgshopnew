package com.shop.admin.product.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.product.dto.supplies.AddSupplyProductDto;
import com.shop.product.dto.supplies.MakerCreateRequestDto;
import com.shop.product.dto.supplies.MakerDto;
import com.shop.product.dto.supplies.SupplyDto;
import com.shop.product.dto.supplies.SupplyTypeCreateRequestDto;
import com.shop.product.dto.supplies.SupplyTypeDto;
import com.shop.product.dto.supplies.UpdateSupplyProductDto;
import com.shop.product.service.supply.AdminSupplyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/product/supply-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupplyProductController {

    private final AdminSupplyService adminSupplyService;
    // ------------------------------------------------------
    // 서플라이 상품 관리
    // ------------------------------------------------------
    @PostMapping
    public ResponseEntity<Void> createSupply(@ModelAttribute AddSupplyProductDto request) throws IOException {
        adminSupplyService.createSupply(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<SupplyDto>> getSupplyList(
            Pageable pageable,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(adminSupplyService.getSupplyListForAdmin(pageable, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplyDto> getSupply(@PathVariable Long id) {
        return ResponseEntity.ok(adminSupplyService.getSupplyByIdForAdmin(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateSupply(
            @PathVariable Long id,
            @ModelAttribute UpdateSupplyProductDto request) throws IOException {
        adminSupplyService.updateSupply(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<Void> deleteSupply(@PathVariable Long id) {
        adminSupplyService.deleteSupply(id);
        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------
    // 서플라이 제조사 관리
    // ------------------------------------------------------
    // 등록 API
    @PostMapping("/makers")
    public ResponseEntity<MakerDto> createMaker(@RequestBody MakerCreateRequestDto request) {
        adminSupplyService.createMaker(request);
        return ResponseEntity.ok().build();
    }

    // 수정 API
    @PutMapping("/makers/{id}")
    public ResponseEntity<MakerDto> updateMaker(@PathVariable Long id, @RequestBody MakerCreateRequestDto request) {
        adminSupplyService.updateMaker(id, request.getName());
        return ResponseEntity.ok().build();
    }

    // 삭제 API
    @PostMapping("/makers/delete/{id}")
    public ResponseEntity<MakerDto> deleteMaker(@PathVariable Long id) {
        adminSupplyService.deleteMaker(id);
        return ResponseEntity.ok().build();
    }

    // TODO: 서플라이 제조사 검색

    @GetMapping("/makers")
    public ResponseEntity<List<MakerDto>> getMakerList() {
        return ResponseEntity.ok(adminSupplyService.getMakerListForAdmin());
    }

    // 목록 조회(페이지용)
    @GetMapping("/makers-by-page")
    public ResponseEntity<Page<MakerDto>> getMakerListByPage(Pageable pageable) {
        return ResponseEntity.ok(adminSupplyService.getMakerListByPageForAdmin(pageable));
    }

    // ------------------------------------------------------
    // 서플라이 타입 관리
    // ------------------------------------------------------

    @PostMapping("/supply-types")
    public ResponseEntity<Void> createCategory(@RequestBody SupplyTypeCreateRequestDto request) {
        adminSupplyService.createSupplyType(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/supply-types/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable Long id, @RequestBody SupplyTypeCreateRequestDto request) {
        adminSupplyService.updateSupplyType(id, request.getNameEn(), request.getNameKo());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/supply-types/delete/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        adminSupplyService.deleteSupplyType(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/supply-types")
    public ResponseEntity<List<SupplyTypeDto>> getSupplyTypeList() {
        return ResponseEntity.ok(adminSupplyService.getSupplyTypeListForAdmin());
    }

    @GetMapping("/supply-types-by-page")
    public ResponseEntity<Page<SupplyTypeDto>> getSupplyTypeListByPage(Pageable pageable) {
        return ResponseEntity.ok(adminSupplyService.getSupplyTypeListByPageForAdmin(pageable));
    }

}
