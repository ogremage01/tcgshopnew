package com.shop.log.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "user_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLog {

    // 사용자 로그 엔티티

    // 사용자 로그 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 카테고리
    private String category;

    // 사용자 ID
    private Long userId;

    // 액션
    private String action;

    // 액션 일시
    private LocalDateTime actionDate;

    // 사용자 이름
    private String userName;

    // 실행자
    private String executor;

    // 성공 여부
    private Boolean isSuccess;

}
