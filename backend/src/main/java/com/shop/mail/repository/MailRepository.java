package com.shop.mail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shop.mail.entity.Mail;

public interface MailRepository extends JpaRepository<Mail, Long> {

    /**
     * 이메일 발송 이력 저장
     */
    // 인증상태 변경

}
