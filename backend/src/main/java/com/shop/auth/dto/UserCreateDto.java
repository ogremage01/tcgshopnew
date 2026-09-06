package com.shop.auth.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateDto {
    // 회원 생성 요청 정보
    private String name;
    private String email;
    private String password;
    private Boolean termsAgreed;
    private LocalDateTime termsAgreedAt;
    private Boolean privacyAgreed;
    private LocalDateTime privacyAgreedAt;
    private String privacyVersion;
    private String termsVersion;
    private String userMemo;
    private String userStatus;
    private String authProvider;
    private String role;
}
