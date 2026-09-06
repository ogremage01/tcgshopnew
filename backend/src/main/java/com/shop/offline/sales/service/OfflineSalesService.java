package com.shop.offline.sales.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.offline.sales.dto.OfflineSalesInfoDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryDto;
import com.shop.offline.sales.dto.OfflineSalesSummaryPeriod;

public interface OfflineSalesService {
    /**
     * 기간별 오프라인 매출 다운로드(동기). 스케줄러용.
     * @return 완료했으면 true, 이미 다른 다운로드(수동/스케줄)가 진행 중이면 false
     */
    boolean downloadOfflineSalesbyPeriod(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 기간별 오프라인 매출 다운로드를 백그라운드에서 시작한다. 수동 API용.
     * 잠금을 선점한 뒤 비동기로 실행한다.
     * @return 시작했으면 true, 이미 진행 중이면 false
     */
    boolean startDownloadOfflineSalesbyPeriod(LocalDateTime startDate, LocalDateTime endDate);

    /** 수동/스케줄 다운로드가 진행 중인지 여부 */
    boolean isDownloadRunning();

    Page<OfflineSalesInfoDto> getOfflineSalesList(Pageable pageable);

    Page<OfflineSalesSummaryDto> getOfflineSalesSummary(OfflineSalesSummaryPeriod period, Pageable pageable);

    Optional<OfflineSalesSummaryDto> getOfflineSalesSummaryReport(
            OfflineSalesSummaryPeriod period,
            LocalDate periodStart);

    Page<OfflineSalesInfoDto> getOfflineSalesList(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
