package com.shop.admin.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.admin.product.dto.card.CardProductManagementResponseDto;
import com.shop.admin.product.dto.card.CardProductAdminSearchResponseDto;
import com.shop.admin.product.dto.card.CardProductPatchRequest;
import com.shop.admin.product.dto.card.CardProductRegister;
import com.shop.card.dto.slim.UnionPriceAdminSearchResponseDto;
import com.shop.admin.product.dto.card.SearchBySetDto;
import com.shop.common.excel.dto.ExcelFileResult;
import com.shop.common.excel.dto.ExcelRequestDto;
import com.shop.common.excel.dto.ExcelUploadResultDto;
import com.shop.common.excel.service.ExcelService;
import com.shop.product.dto.card.management.CardProductManagementDto;
import com.shop.product.dto.card.management.SearchBySetCriteriaDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import com.shop.admin.product.service.PriceErrorCardService;
import com.shop.product.service.CardProductService;
import com.shop.card.service.UnionPriceQueryService;
import com.shop.scheduler.price.service.union.UnionPriceIngestionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/admin/product/single-products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSingleProductController {
    private final ExcelService excelService;
    private final CardProductService cardProductService;
    private final UnionPriceQueryService unionPriceQueryService;
    private final UnionPriceIngestionService unionPriceIngestionService;
    private final PriceErrorCardService priceErrorCardService;

    // TODO: 상품 상세 조회

    // TODO: 상품 필터(상품 종류)

    // ------------------------------------------------------
    // 단건 상품 관리
    // ------------------------------------------------------

    // 유니온 가격 검색 조회
    @GetMapping("/search/cards")
    public ResponseEntity<UnionPriceAdminSearchResponseDto> searchUnionPrices(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "game", required = false) String game,
            @RequestParam(value = "setCode", required = false) String setCode,
            @PageableDefault(size = 12, sort = "setNumber", direction = Sort.Direction.ASC) Pageable pageable) {
        UnionPriceAdminSearchResponseDto unionPrices = unionPriceQueryService.searchByKeywordForAdmin(keyword, game, setCode,
                pageable);
        return ResponseEntity.ok(unionPrices);
    }

    // 카드 검색 조회
    @GetMapping("/search/products")
    public ResponseEntity<CardProductAdminSearchResponseDto> searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "game", required = false) String game,
            @RequestParam(value = "setCode", required = false) String setCode,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(cardProductService.searchByKeywordForAdmin(keyword, game, setCode, pageable));
    }

    // 카드 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<Page<CardProductManagementResponseDto>> getProduct(
            @PathVariable("id") Long productId, Pageable pageable) {
        Page<CardProductManagementResponseDto> result = cardProductService
                .findByProductIdAndIsDeleted(productId, false, pageable)
                .map(CardProductManagementResponseDto::from);
        return ResponseEntity.ok(result);
    }

    // 카드 단건 수정
    @PatchMapping("/{id}")
    public ResponseEntity<CardProductManagementResponseDto> patchCardProduct(
            @PathVariable("id") Long id,
            @RequestBody CardProductPatchRequest body) {
        CardProductManagementDto result = cardProductService.patchCardProduct(id, body.toCommand());
        return ResponseEntity.ok(CardProductManagementResponseDto.from(result));
    }

    // 카드 단건 등록
    @PostMapping("/card")
    public ResponseEntity<String> addCardProduct(
            @RequestBody CardProductRegister cardProductRegister) {
        cardProductService.registerCardProduct(cardProductRegister.toCommand());
        return ResponseEntity.ok("카드 등록 완료");
    }

    // 카드 단건 삭제
    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteCardProduct(
            @PathVariable("id") Long id) {
        cardProductService.deleteCardProductById(id);
        return ResponseEntity.ok("카드 삭제 완료");
    }

    // ------------------------------------------------------
    // 대량 업로드 관리
    // ------------------------------------------------------
    // 엑셀 파일 업로드
    @PostMapping("/multiple/upload")
    public ResponseEntity<ExcelUploadResultDto> uploadMultipleProductExcelFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        try {
            return ResponseEntity.ok(excelService.saveProductExcelFile(file));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/multiple/download")
    public ResponseEntity<byte[]> downloadExcelFile(@RequestBody ExcelRequestDto excelRequestDto) {
        ExcelFileResult excelFileResult = excelService.getProductExcelFile(excelRequestDto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileResult.fileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFileResult.bytes());
    }

    // ------------------------------------------------------
    // ProductSearchMap ← UnionPrice 전체 백필 (기존 데이터 일회성 동기화)
    // ------------------------------------------------------
    @PostMapping("/sync/union-price-search-map")
    public ResponseEntity<String> syncUnionPriceSearchMap() {
        boolean started = unionPriceIngestionService.backfillProductSearchMapFromUnionPrices();
        if (!started) {
            return ResponseEntity.status(409).body("이미 동기화가 진행 중입니다.");
        }
        return ResponseEntity.ok("UnionPrice → ProductSearchMap 동기화가 시작되었습니다.");
    }

    @PostMapping("/sync/product-search-map-reference-axis")
    public ResponseEntity<String> rebuildProductSearchMapByReferenceAxis() {
        boolean started = unionPriceIngestionService.rebuildProductSearchMapsByReferenceAxis();
        if (!started) {
            return ResponseEntity.status(409).body("ProductSearchMap 리빌드가 이미 진행 중입니다.");
        }
        return ResponseEntity.ok("ProductSearchMap 참조축 리빌드가 시작되었습니다.");
    }

    // ------------------------------------------------------
    // 가격 오류 카드 관리
    // ------------------------------------------------------

    @GetMapping("/price-error-cards")
    public ResponseEntity<Page<CardProductManagementResponseDto>> getPriceErrorCards(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(priceErrorCardService.getPriceErrorCards(pageable));
    }

    // ------------------------------------------------------
    // 세트별 싱글카드 현황 조회 modelAttribute 사용 이유: Dto를 사용하면 쿼리 파라미터로 전달되어 쿼리 파라미터 타입을 지정
    // ------------------------------------------------------
    @GetMapping("/search/by-set")
    public ResponseEntity<Page<CardProductManagementResponseDto>> searchBySet(
            @ModelAttribute SearchBySetDto searchBySetDto,
            @PageableDefault(size = 20) Pageable pageable) {
        SearchBySetCriteriaDto criteria = SearchBySetCriteriaDto.builder()
                .game(searchBySetDto.getGame())
                .set(searchBySetDto.getSet())
                .printTypeFilter(searchBySetDto.getPrintTypeFilter())
                .storageId(searchBySetDto.getStorageId())
                .build();
        Page<CardProductManagementResponseDto> result = cardProductService
                .searchBySetForAdmin(criteria, pageable)
                .map(CardProductManagementResponseDto::from);
        return ResponseEntity.ok(result);
    }
}
