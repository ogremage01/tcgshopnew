package com.shop.log.sync.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SyncLogDto {
    // 동기화 로그 정보
    private Long id;
    private String syncSource;
    private String syncTarget;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long proceedingTime;
    private String result; // success, failure, partial_success
    private String message;
}