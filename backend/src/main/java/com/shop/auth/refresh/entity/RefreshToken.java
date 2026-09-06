package com.shop.auth.refresh.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "refresh_tokens")
public class RefreshToken {
    // 리프레시 토큰 엔티티
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = true)
    private LocalDateTime revokedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime rotatedAt;

    @PrePersist
    protected void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Service에서 사용하는 최소 생성자.
     * revokedAt/rotatedAt은 기본적으로 null입니다.
     */
    public RefreshToken(Long userId, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    /**
     * Refresh token을 더 이상 사용하지 못하도록 비활성화합니다.
     * revokedAt이 null이 아닌 상태는 Repository 조회 조건에서 제외됩니다.
     */
    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    /**
     * refresh token 회전(rotate) 처리 시각을 기록합니다.
     * (기존 token revoke와 함께 사용되는 경우가 많습니다.)
     */
    public void rotate(LocalDateTime rotatedAt) {
        this.rotatedAt = rotatedAt;
    }

}
