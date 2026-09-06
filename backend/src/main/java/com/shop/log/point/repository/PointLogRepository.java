package com.shop.log.point.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.log.point.entity.PointLog;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    List<PointLog> findTop50ByOrderByActionDateDesc();

    List<PointLog> findTop10ByOrderByActionDateDesc();

    Page<PointLog> findByUserIdOrderByActionDateDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT p
            FROM PointLog p
            WHERE (:includeSystem = true OR p.actorType IS NULL
                   OR p.actorType <> com.shop.log.point.entity.PointLogActorType.SYSTEM)
              AND (:keyword IS NULL OR :keyword = '' OR p.userName LIKE %:keyword%
                   OR p.userId IN (SELECT u.id FROM User u WHERE u.email LIKE %:keyword% OR u.userMemo LIKE %:keyword%))
            ORDER BY p.actionDate DESC
            """)
    Page<PointLog> findAllByAdminFilter(@Param("keyword") String keyword, @Param("includeSystem") boolean includeSystem,
            Pageable pageable);

    boolean existsByChangeReason(String changeReason);
}
