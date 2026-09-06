package com.shop.admin.user.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserManagementResponseDto {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Long point;
    private String userStatus;
    private String userMemo;
    private LocalDateTime createdAt;
}
