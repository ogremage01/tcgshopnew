package com.shop.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private AuthUserDto user;

    /**
     * refresh token 원문은 응답 body로 내려보내지 않음 (HttpOnly 쿠키로만 전달)
     * - 프론트에서는 사용하지 않음
     */
    @JsonIgnore
    private String refreshToken;
}
