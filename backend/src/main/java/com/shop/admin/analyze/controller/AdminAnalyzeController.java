package com.shop.admin.analyze.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.admin.analyze.dto.AdminDailyReportDto;
import com.shop.admin.analyze.dto.AdminDailySalesReportRowDto;
import com.shop.admin.analyze.dto.AdminDailySalesSummarySimpleDto;
import com.shop.admin.analyze.dto.AdminMonthlySalesReportDto;
import com.shop.admin.analyze.dto.AdminMonthlySalesReportRowDto;
import com.shop.admin.analyze.dto.AdminOfflineSalesTotalDto;
import com.shop.admin.analyze.dto.AdminSalesSummarySimpleDto;
import com.shop.admin.analyze.dto.AdminStockSummaryDto;
import com.shop.admin.analyze.dto.AdminWeeklyReportDto;
import com.shop.admin.analyze.dto.AdminWeeklySalesReportRowDto;
import com.shop.admin.analyze.service.AdminAnalyzeService;
import com.shop.common.excel.dto.ExcelFileResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/analyze")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyzeController {
    private final AdminAnalyzeService adminAnalyzeService;

//TODO: 당월 매출 현황
@GetMapping("/current-month-sales-summary")
public ResponseEntity<AdminSalesSummarySimpleDto> getCurrentMonthSalesSummary() {
    return ResponseEntity.ok(adminAnalyzeService.getCurrentMonthSalesSummary());
}

//TODO: 주문 현황


@GetMapping("/stock-summary")
public ResponseEntity<List<AdminStockSummaryDto>> getStockSummary() {
    return ResponseEntity.ok(adminAnalyzeService.getStockSummary());
}

@GetMapping("/total-sales-summary")
public ResponseEntity<AdminSalesSummarySimpleDto> getTotalSalesSummary() {
    return ResponseEntity.ok(adminAnalyzeService.getTotalSalesSummary());
}

@GetMapping("/daily-sales-summary")
public ResponseEntity<Page<AdminDailySalesSummarySimpleDto>> getDailySalesSummary(
        @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(adminAnalyzeService.getDailySalesSummary(pageable));
}

@GetMapping("/daily-sales-report-list")
public ResponseEntity<Page<AdminDailySalesReportRowDto>> getDailySalesReportList(
        @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(adminAnalyzeService.getDailySalesReportList(pageable));
}

@GetMapping("/daily-sales-report/{day}")
public ResponseEntity<AdminDailyReportDto> getDailySalesReport(@PathVariable String day) {
    return ResponseEntity.ok(adminAnalyzeService.getDailySalesReport(day));
}

@GetMapping("/weekly-sales-report-list")
public ResponseEntity<Page<AdminWeeklySalesReportRowDto>> getWeeklySalesReportList(
        @PageableDefault(size = 20, sort = "monday", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(adminAnalyzeService.getWeeklySalesReportList(pageable));
}

@GetMapping("/weekly-sales-report/{monday}")
public ResponseEntity<AdminWeeklyReportDto> getWeeklySalesReport(@PathVariable String monday) {
    return ResponseEntity.ok(adminAnalyzeService.getWeeklySalesReport(monday));
}

@GetMapping("/monthly-sales-report-list")
public ResponseEntity<Page<AdminMonthlySalesReportRowDto>> getMonthlySalesReportList(
        @PageableDefault(size = 20, sort = "month", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(adminAnalyzeService.getMonthlySalesReportList(pageable));
}

@GetMapping("/monthly-sales-report/{month}")
public ResponseEntity<AdminMonthlySalesReportDto> getMonthlySalesReport(@PathVariable String month) {
    return ResponseEntity.ok(adminAnalyzeService.getMonthlySalesReport(month));
}

@GetMapping("/offline-sales-total")
public ResponseEntity<Page<AdminOfflineSalesTotalDto>> getOfflineSalesTotal(
        @PageableDefault(size = 20) Pageable pageable,
        @RequestParam String startDate,
        @RequestParam String endDate) {
    return ResponseEntity.ok(adminAnalyzeService.getOfflineSalesTotal(pageable, startDate, endDate));
}

@PostMapping("/offline-sales-total/download")
public ResponseEntity<byte[]> downloadOfflineSalesTotal(
        @RequestParam String startDate,
        @RequestParam String endDate) {
    ExcelFileResult excelFileResult = adminAnalyzeService.downloadOfflineSalesTotal(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + excelFileResult.fileName())
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFileResult.bytes());
}

}
