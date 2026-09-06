package com.shop.user.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    // 회원 요청 DTO

    private String name;
    // private String address;
    // private String phone;
    private String password;
    private String passwordConfirm;
}
