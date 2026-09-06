package com.shop.log.user.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLogDto {

    // 사용자 로그 정보
    private Long id;

    private String category;

    private Long userId;

    private String action;

    private LocalDateTime actionDate;

    private String userName;

    private String executor;

    private Boolean isSuccess;


}
