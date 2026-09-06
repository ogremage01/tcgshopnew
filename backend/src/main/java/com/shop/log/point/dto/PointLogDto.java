package com.shop.log.point.dto;

import java.time.LocalDateTime;

import com.shop.log.point.entity.PointLogActorType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLogDto {

    private Long id;

    private Long userId;

    private String userName;

    private Long changedPoint;

    private Long beforePoint;

    private Long afterPoint;

    private String changeReason;

    private PointLogActorType actorType;

    /** 감사용 원본 (관리자 이름·이메일 등) */
    private String actorDetail;

    /** 관리자 화면 표시용 (이름·이메일 등으로 resolve) */
    private String executor;

    private LocalDateTime actionDate;

    private Boolean isSuccess;
}
