package com.shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // JWT 유틸

    private final SecretKey key;
    private final long expirationMs;

    /**
     * JWT 유틸 생성
     * 
     * @param secret       JWT 시크릿
     * @param expirationMs JWT 만료 시간
     */
    public JwtUtil(@Value("${app.jwt.secret:${JWT_SECRET:dev-only-jwt-secret-key-change-me-please-32bytes-min}}") String secret,
            @Value("${app.jwt.expiration-ms:${JWT_EXPIRATION_MS:86400000}}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * JWT 토큰 생성
     * 
     * @param subject JWT 제목
     * @return JWT 토큰
     */
    public String generateToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * JWT 토큰 파싱
     * 
     * @param token JWT 토큰
     * @return JWT 토큰 페이로드
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT 토큰 검증
     * 
     * @param token JWT 토큰
     * @return JWT 토큰 검증 결과
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
