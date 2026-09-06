package com.shop.log.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.log.user.entity.UserLog;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    Optional<UserLog> findTopByUserIdOrderByActionDateDesc(Long userId);

    Page<UserLog> findAllByUserId(Long userId, Pageable pageable);
    Page<UserLog> findAllByCategory(String category, Pageable pageable);
    Page<UserLog> findAllByAction(String action, Pageable pageable);
    Page<UserLog> findAllByIsSuccess(Boolean isSuccess, Pageable pageable);

    void deleteByUserId(Long userId);

}
