package com.shop.admin.offlineProduct.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.common.excel.dto.ExcelFileResult;
import com.shop.offline.product.dto.OfflineProductDto;
import com.shop.offline.product.dto.OfflineProductReceivingHistoryDto;
import com.shop.offline.product.dto.OfflineProductReceivingRequest;
import com.shop.offline.product.dto.OfflineShippingReconcileResultDto;
import com.shop.offline.product.dto.OfflineStockSyncResultDto;
import com.shop.offline.product.dto.PackagingUnitDto;
import com.shop.offline.product.dto.PackagingUnitRequest;
import com.shop.offline.product.dto.SimpleRegisterOfflineProductDto;
import com.shop.offline.product.service.OfflineProductService;
import com.shop.offline.product.service.PackagingUnitService;
import com.shop.offline.sales.dto.OfflineSalesInfoDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryPeriod;
import com.shop.offline.sales.service.OfflineSalesService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/admin/offline-data")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOfflineDataController {

    private final OfflineProductService offlineProductService;
    private final OfflineSalesService offlineSalesService;
    private final PackagingUnitService packagingUnitService;
    @GetMapping("/product")
    public ResponseEntity<Page<OfflineProductDto>> getOfflineProductList(
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            Pageable pageable) {
        return ResponseEntity.ok(
                offlineProductService.getOfflineProductList(sortBy, sortDirection, pageable));
    }

    @PostMapping("/product/download")
    public ResponseEntity<Void> downloadOfflineProducts() {
        offlineProductService.downloadOfflineProducts();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/product/download-excel")
    public ResponseEntity<byte[]> downloadOfflineProductsExcel() {
        ExcelFileResult excelFileResult = offlineProductService.downloadOfflineProductsExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileResult.fileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFileResult.bytes());
    }

    @GetMapping("/product/search")
    public ResponseEntity<Page<OfflineProductDto>> searchOfflineProductList(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection,
            Pageable pageable) {
        return ResponseEntity.ok(
                offlineProductService.getOfflineProductListByKeyword(
                        keyword, sortBy, sortDirection, pageable));
    }

    @GetMapping("/product/by-product-id")
    public ResponseEntity<OfflineProductDto> getOfflineProductByProductId(
            @RequestParam("productId") String productId) {
        return ResponseEntity.ok(offlineProductService.getOfflineProductByProductId(productId));
    }

    @PostMapping("/product/simple-register")
    public ResponseEntity<Void> simpleRegisterOfflineProduct(@RequestBody SimpleRegisterOfflineProductDto simpleRegisterOfflineProductDto) {
        offlineProductService.simpleRegisterOfflineProduct(simpleRegisterOfflineProductDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/product/receiving")
    public ResponseEntity<OfflineProductReceivingHistoryDto> registerReceiving(
            @RequestBody OfflineProductReceivingRequest request) {
        return ResponseEntity.ok(offlineProductService.registerReceiving(request));
    }

    @GetMapping("/product/receiving")
    public ResponseEntity<Page<OfflineProductReceivingHistoryDto>> getReceivingHistories(Pageable pageable) {
        return ResponseEntity.ok(offlineProductService.getReceivingHistories(pageable));
    }

    @GetMapping("/product/receiving/{id}")
    public ResponseEntity<OfflineProductReceivingHistoryDto> getReceivingHistory(@PathVariable("id") Long id) {
        return ResponseEntity.ok(offlineProductService.getReceivingHistory(id));
    }

    /**
     * COMPLETED 매출 기준 shippingQuantity 전량 절대값 재집계 (idempotent).
     * cutover/드리프트 보정용. 일상 sync 경로와 별개.
     */
    @PostMapping("/product/shipping/reconcile")
    public ResponseEntity<OfflineShippingReconcileResultDto> reconcileShippingQuantities() {
        return ResponseEntity.ok(offlineProductService.reconcileShippingQuantities());
    }

    /**
     * 현재 입고/출고만으로 묶음 전환 + 온라인 재고 동기화.
     * 출고 절대값 재집계는 하지 않는다.
     */
    @PostMapping("/product/stock/sync")
    public ResponseEntity<OfflineStockSyncResultDto> syncStockFromCurrentQuantities() {
        return ResponseEntity.ok(offlineProductService.syncStockFromCurrentQuantities());
    }

    //----------------------------------
    // 포장 단위 (PackagingUnit)
    //----------------------------------

    @GetMapping("/packaging-units")
    public ResponseEntity<Page<PackagingUnitDto>> listPackagingUnits(Pageable pageable) {
        return ResponseEntity.ok(packagingUnitService.list(pageable));
    }

    @GetMapping("/packaging-units/{id}")
    public ResponseEntity<PackagingUnitDto> getPackagingUnit(@PathVariable("id") Long id) {
        return ResponseEntity.ok(packagingUnitService.get(id));
    }

    @PostMapping("/packaging-units")
    public ResponseEntity<PackagingUnitDto> createPackagingUnit(@RequestBody PackagingUnitRequest request) {
        return ResponseEntity.ok(packagingUnitService.create(request));
    }

    @PutMapping("/packaging-units/{id}")
    public ResponseEntity<PackagingUnitDto> updatePackagingUnit(
            @PathVariable("id") Long id,
            @RequestBody PackagingUnitRequest request) {
        return ResponseEntity.ok(packagingUnitService.update(id, request));
    }

    @DeleteMapping("/packaging-units/{id}")
    public ResponseEntity<Void> deletePackagingUnit(@PathVariable("id") Long id) {
        packagingUnitService.delete(id);
        return ResponseEntity.ok().build();
    }

    //----------------------------------
    // 오프라인 매출 현황
    //----------------------------------

    @GetMapping("/sales")
    public ResponseEntity<Page<OfflineSalesInfoDto>> getOfflineSalesList(Pageable pageable) {
        return ResponseEntity.ok(offlineSalesService.getOfflineSalesList(pageable));
    }

    @GetMapping("/sales/summary")
    public ResponseEntity<Page<OfflineSalesSummaryDto>> getOfflineSalesSummary(
            @RequestParam("period") String period,
            Pageable pageable) {
        return ResponseEntity.ok(
                offlineSalesService.getOfflineSalesSummary(OfflineSalesSummaryPeriod.from(period), pageable));
    }

    @GetMapping("/sales/summary/report")
    public ResponseEntity<OfflineSalesSummaryDto> getOfflineSalesSummaryReport(
            @RequestParam("period") String period,
            @RequestParam(value = "periodStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        OfflineSalesSummaryPeriod summaryPeriod = OfflineSalesSummaryPeriod.from(period);
        if (summaryPeriod != OfflineSalesSummaryPeriod.TOTAL && periodStart == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<OfflineSalesSummaryDto> report = offlineSalesService.getOfflineSalesSummaryReport(
                summaryPeriod,
                periodStart);
        return report.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sales/download")
    public ResponseEntity<String> downloadOfflineSalesbyPeriod(
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("endDate") LocalDateTime endDate) {
        boolean started = offlineSalesService.startDownloadOfflineSalesbyPeriod(startDate, endDate);
        if (!started) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 다운로드가 진행 중입니다.");
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("오프라인 매출 다운로드를 백그라운드에서 시작했습니다.");
    }
    @GetMapping("/sales/list")
    public ResponseEntity<Page<OfflineSalesInfoDto>> getOfflineSalesList(
            @RequestParam("startDate") LocalDateTime startDate,
            @RequestParam("endDate") LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(offlineSalesService.getOfflineSalesList(startDate, endDate, pageable));
    }
}
