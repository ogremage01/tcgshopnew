package com.shop.auth.refresh.repository;

import com.shop.auth.refresh.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 리프레시 토큰 조회
    /**
     * 리프레시 토큰 조회
     * 
     * @param tokenHash 토큰 해시
     * @param now       현재 시간
     * @return 리프레시 토큰
     */
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    /**
     * 리프레시 토큰 삭제
     * 
     * @param now 현재 시간
     */
    void deleteByExpiresAtBefore(LocalDateTime now);

    void deleteByUserId(Long userId);
}
