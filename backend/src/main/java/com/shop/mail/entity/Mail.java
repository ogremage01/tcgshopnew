package com.shop.mail.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import java.time.LocalDateTime;

@Entity
@Table(name = "mails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mail {
    // 메일 엔티티

    // 메일 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 수신처
    @Column(name = "mail_to", nullable = false)
    private String to;

    // 인증 여부
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    // 생성 시간
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
