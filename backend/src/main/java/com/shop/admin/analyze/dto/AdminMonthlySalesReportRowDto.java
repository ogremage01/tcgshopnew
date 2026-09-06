package com.shop.admin.analyze.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMonthlySalesReportRowDto {
    /** 해당 월의 1일 (YYYY-MM-DD) */
    private LocalDate month;
    private long totalAmount;
    private long onlineAmount;
    private long offlineAmount;
}
