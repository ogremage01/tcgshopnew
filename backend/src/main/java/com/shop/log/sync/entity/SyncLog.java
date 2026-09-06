package com.shop.log.sync.entity;

import com.shop.common.jpa.DurationNanosConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.time.Duration;
import com.shop.log.sync.dto.SyncLogDto;

@Entity
@Table(name = "sync_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {

    // 동기화 로그 엔티티

    // 동기화 로그 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 동기화 소스(tcgplayer, cardkingdom...) */
    private String syncSource;

    /** 동기화 대상(productLines, productTypes, setNames, prices) */
    private String syncTarget;
    /** 동기화 시작 시간 */
    private LocalDateTime startTime;
    /** 동기화 종료 시간 */
    private LocalDateTime endTime;
    /** 동기화 실행 시간 (DB: BIGINT 나노초) */
    @Convert(converter = DurationNanosConverter.class)
    private Duration proceedingTime;
    /** 동기화 성공 여부 (success, failure, partial_success) */
    private String result;

    /** 동기화 메시지(혹은 실패 사유 등) */
    @Column(columnDefinition = "TEXT")
    private String message;

    public SyncLogDto toDto() {
        return new SyncLogDto(id, syncSource, syncTarget, startTime, endTime, proceedingTime.toSeconds(), result,
                message);
    }
}
