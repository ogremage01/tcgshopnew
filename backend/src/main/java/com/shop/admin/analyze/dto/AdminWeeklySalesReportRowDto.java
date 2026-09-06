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
public class AdminWeeklySalesReportRowDto {
    /** 해당 주의 월요일 (YYYY-MM-DD) */
    private LocalDate monday;
    private long totalAmount;
    private long onlineAmount;
    private long offlineAmount;
}
