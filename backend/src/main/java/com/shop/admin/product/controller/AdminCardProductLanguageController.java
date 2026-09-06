package com.shop.admin.product.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.product.dto.card.CardProductLanguageDto;
import com.shop.product.service.CardProductLanguageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/product/card-product-languages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCardProductLanguageController {

    private final CardProductLanguageService cardProductLanguageService;

    @GetMapping
    public ResponseEntity<List<CardProductLanguageDto>> getCardProductLanguages() {
        return ResponseEntity.ok(cardProductLanguageService.findActiveLanguages());
    }
}
