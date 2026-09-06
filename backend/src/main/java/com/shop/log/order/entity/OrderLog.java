package com.shop.log.order.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문 ID
    private Long orderId;

    // 카테고리
    private String category;

    // 액션
    private String action;

    // 액션 일시
    private LocalDateTime actionDate;

    // 사용자 ID
    private Long userId;

    // 사용자 이름
    private String userName;

    // 성공 여부

    private Boolean isSuccess;

}