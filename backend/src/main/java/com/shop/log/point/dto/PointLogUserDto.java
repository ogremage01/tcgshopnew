package com.shop.log.point.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 고객 마이페이지용 포인트 로그 (실행자 개인정보 미포함).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLogUserDto {

    private Long id;

    private Long changedPoint;

    private Long beforePoint;

    private Long afterPoint;

    private String changeReason;

    /** 고객 노출용: 관리자, 시스템, 본인 등 */
    private String executorDisplay;

    private LocalDateTime actionDate;

    private Boolean isSuccess;
}
