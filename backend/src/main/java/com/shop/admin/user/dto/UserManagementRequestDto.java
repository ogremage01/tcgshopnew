package com.shop.admin.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementRequestDto {

    private Long id;
    private String name;
    private String role;
    private String userStatus;
    private String userMemo;
    private String password;
    private String passwordConfirm;
}
