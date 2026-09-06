package com.shop.auth.dto;

import com.shop.user.dto.user.UserResponseDto;
import com.shop.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인·리프레시 응답에 포함되는 사용자 요약.
 * JWT subject와 동일한 {@code publicId}를 클라이언트 식별자로 사용한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUserDto {

    private String publicId;
    private String email;
    private String name;
    private String role;
    private Long point;

    public static AuthUserDto fromEntity(User user) {
        return AuthUserDto.builder()
                .publicId(user.getPublicId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .point(user.getPoint() != null ? user.getPoint() : 0L)
                .build();
    }

    public static AuthUserDto fromUserResponse(UserResponseDto user) {
        if (user == null) {
            return null;
        }
        return AuthUserDto.builder()
                .publicId(user.getPublicId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .point(user.getPoint() != null ? user.getPoint() : 0L)
                .build();
    }
}
