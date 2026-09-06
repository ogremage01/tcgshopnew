package com.shop.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import com.shop.common.util.UlidGenerator;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    // 회원 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공개 식별자(ULID). JWT subject·비관리 API 조회에 사용. */
    @Column(nullable = false, unique = true, length = 26)
    private String publicId;

    // 이름
    @Column(nullable = false)
    private String name;
    // 이메일
    @Column(nullable = false, unique = true)
    private String email;
    // 비밀번호
    @Column(nullable = true, length = 100)
    private String password;
    // 권한
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ROLE_USER'")
    private String role;
    // 생성 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 수정 시간
    @LastModifiedDate
    @Column(nullable = false, updatable = true)
    private LocalDateTime updatedAt;
    // 포인트
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long point;
    // 메모
    @Column(nullable = true, columnDefinition = "VARCHAR(200) DEFAULT ''")
    private String userMemo;
    // 사용자 상태
    // ACTIVE, INACTIVE, DELETED, BANNED
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private String userStatus;

    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'LOCAL'")
    private String authProvider;
    // 약관 동의 여부
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean termsAgreed;
    // 약관 동의 시간
    @Column(nullable = false)
    private LocalDateTime termsAgreedAt;
    // 약관 버전
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT ''")
    private String termsVersion;
    // 개인정보 동의 여부
    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean privacyAgreed;
    // 개인정보 동의 시간
    @Column(nullable = false)
    private LocalDateTime privacyAgreedAt;
    // 개인정보 버전
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT ''")
    private String privacyVersion;

    @PrePersist
    void assignPublicId() {
        if (publicId == null || publicId.isBlank()) {
            publicId = UlidGenerator.nextUlid();
        }
    }
}
