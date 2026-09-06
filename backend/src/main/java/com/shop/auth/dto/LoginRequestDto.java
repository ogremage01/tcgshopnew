package com.shop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    // 로그인 요청 정보
    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
