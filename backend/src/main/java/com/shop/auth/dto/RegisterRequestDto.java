package com.shop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 회원가입 요청 정보 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {
    private String name;
    private String email;
    // private String address;
    // private String phone;
    private String password;
    private String passwordConfirm;
}
