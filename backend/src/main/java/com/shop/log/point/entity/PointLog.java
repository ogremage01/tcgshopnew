package com.shop.log.point.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String userName;

    private Long changedPoint;

    private Long beforePoint;

    private Long afterPoint;

    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type")
    private PointLogActorType actorType;

    /**
     * 감사용 상세: ADMIN/USER는 {@code 이름 (이메일)} 등, SYSTEM은 job 식별자(선택).
     * 기존 DB 컬럼명 executor와 호환.
     */
    @Column(name = "executor")
    private String actorDetail;

    private LocalDateTime actionDate;

    private Boolean isSuccess;

    /**
     * actor_type 미기입 레거시 행: executor 컬럼 값으로 유형 추론.
     */
    public PointLogActorType resolveActorType() {
        if (actorType != null) {
            return actorType;
        }
        if ("SYSTEM".equals(actorDetail)) {
            return PointLogActorType.SYSTEM;
        }
        return PointLogActorType.ADMIN;
    }
}
