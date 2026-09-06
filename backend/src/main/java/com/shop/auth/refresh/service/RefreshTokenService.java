package com.shop.auth.refresh.service;

public interface RefreshTokenService {

    // 리프레시 토큰 삭제
    /**
     * 리프레시 토큰 삭제
     * 
     * @param now 현재 시간
     */
    void deleteExpiredTokens();
}
