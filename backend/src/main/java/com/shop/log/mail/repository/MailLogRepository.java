package com.shop.log.mail.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.shop.log.mail.entity.MailLog;

public interface MailLogRepository extends JpaRepository<MailLog, Long> {

}
