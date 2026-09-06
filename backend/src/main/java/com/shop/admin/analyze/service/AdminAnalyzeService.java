package com.shop.admin.analyze.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
import com.shop.common.excel.dto.ExcelFileResult;
public interface AdminAnalyzeService {
    List<AdminStockSummaryDto> getStockSummary();
    AdminSalesSummarySimpleDto getCurrentMonthSalesSummary();
    AdminSalesSummarySimpleDto getTotalSalesSummary();
    Page<AdminDailySalesSummarySimpleDto> getDailySalesSummary(Pageable pageable);
    Page<AdminDailySalesReportRowDto> getDailySalesReportList(Pageable pageable);
    AdminDailyReportDto getDailySalesReport(String day);

    Page<AdminWeeklySalesReportRowDto> getWeeklySalesReportList(Pageable pageable);
    AdminWeeklyReportDto getWeeklySalesReport(String monday);

    Page<AdminMonthlySalesReportRowDto> getMonthlySalesReportList(Pageable pageable);
    AdminMonthlySalesReportDto getMonthlySalesReport(String month);

    Page<AdminOfflineSalesTotalDto> getOfflineSalesTotal(Pageable pageable, String startDate, String endDate);
    ExcelFileResult downloadOfflineSalesTotal(String startDate, String endDate);
}
