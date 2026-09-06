package com.shop.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.product.dto.ProductItemDto;
import com.shop.search.dto.searching.ProductSearchingDto;
import com.shop.search.dto.searching.SearchInitResponseDto;
import com.shop.search.dto.suggest.ProductSuggestResponseDto;
import com.shop.search.service.ProductSearchMapService;
import com.shop.search.service.ProductSuggestService;

import lombok.RequiredArgsConstructor;

//상품 통합검색 컨트롤러

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:3000}", allowCredentials = "true")
@RequiredArgsConstructor
public class ProductController {

    private final ProductSearchMapService productSearchMapService;
    private final ProductSuggestService productSuggestService;

    @GetMapping
    public ResponseEntity<Page<ProductItemDto>> list(
            @ModelAttribute ProductSearchingDto query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productSearchMapService.searchProducts(query, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductItemDto>> search(
            @ModelAttribute ProductSearchingDto query,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        if (query.getKeyword() == null || query.getKeyword().isBlank()) {
            if (q != null && !q.isBlank()) {
                query.setKeyword(q);
            }
        }
        return ResponseEntity.ok(productSearchMapService.searchProducts(query, pageable));
    }

    @GetMapping("/suggest")
    public ResponseEntity<ProductSuggestResponseDto> suggest(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(productSuggestService.suggest(q, limit));
    }

    @GetMapping("/search/init")
    public ResponseEntity<SearchInitResponseDto> searchInit(
            @ModelAttribute ProductSearchingDto query,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        if (query.getKeyword() == null || query.getKeyword().isBlank()) {
            if (q != null && !q.isBlank()) {
                query.setKeyword(q);
            }
        }
        return ResponseEntity.ok(productSearchMapService.searchInit(query, pageable));
    }
    @GetMapping("/search/game/{game}/{setCode}")
    public ResponseEntity<Page<ProductItemDto>> searchBySet(
            @PathVariable String game,
            @PathVariable String setCode,
            @ModelAttribute ProductSearchingDto query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productSearchMapService.searchProductsByGameSetCode(game, setCode, query, pageable));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ProductItemDto> getProductDetail(@PathVariable String id) {
        return ResponseEntity.ok(productSearchMapService.getProductDetail(id));
    }
}
