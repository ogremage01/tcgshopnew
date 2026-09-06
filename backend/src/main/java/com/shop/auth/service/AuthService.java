package com.shop.auth.service;

import com.shop.auth.dto.AuthResponseDto;
import com.shop.auth.dto.LoginRequestDto;
import com.shop.auth.dto.RegisterRequestDto;

/**
 * 로그인·인증 관련 비즈니스 로직.
 * 로그인 시 토큰 생성 및 AuthResponseDto 조립은 서비스에서 처리.
 */
public interface AuthService {

    /**
     * 이메일/비밀번호로 로그인하여 토큰과 사용자 정보를 담은 AuthResponseDto 반환.
     *
     * @param request 로그인 요청 (email, password)
     * @return 성공 시 AuthResponseDto, 실패 시 null
     */
    AuthResponseDto login(LoginRequestDto request);

    /**
     * refresh token 검증 후 access token 재발급 + refresh rotate.
     * 
     * @param refreshToken 쿠키에서 전달된 refresh token 원문
     * @return 성공 시 AuthResponseDto (refreshToken은 JsonIgnore)
     */
    AuthResponseDto refresh(String refreshToken);

    /**
     * refresh token revoke 및 쿠키 무효화 처리.
     */
    void logout(String refreshToken);

    /**
     * 회원가입
     *
     * @param request 회원가입 요청 정보
     */
    void register(RegisterRequestDto request);

    /**
     * 인증 메일 발송
     *
     * @param email 이메일
     */
    void sendVerificationEmail(String email);
}
