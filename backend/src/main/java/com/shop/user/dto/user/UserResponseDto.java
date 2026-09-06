package com.shop.user.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {

    // 회원 응답 DTO

    private Long id;
    /** 공개 ULID. 클라이언트·JWT와 동일한 식별자. */
    private String publicId;
    private String name;
    private String email;
    // private String address;
    // private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long point;
    private String role;
}
