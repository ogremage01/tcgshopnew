package com.shop.log.mail.entity;

import jakarta.persistence.Column;
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
@Table(name = "mail_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 수신처
    @Column(name = "mail_to")
    private String to;

    // 카테고리
    private String category;

    // 생성 시간
    private LocalDateTime createdAt;
    // 성공 여부
    private Boolean isSuccess;
}
