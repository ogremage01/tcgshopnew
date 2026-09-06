package com.shop.user.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // 회원 리포지토리

    User findByEmail(String email);

    Optional<User> findByPublicId(String publicId);

    @Modifying
    @Query("UPDATE User SET userStatus = :userStatus, updatedAt = :updatedAt WHERE id = :userId")
    void updateUserStatus(Long userId, String userStatus, LocalDateTime updatedAt);

    // ------------------------------------------------
    // 관리자용 메서드
    // ------------------------------------------------
    @Query("SELECT u FROM User u WHERE (u.name LIKE %:keyword% OR u.email LIKE %:keyword% OR u.userMemo LIKE %:keyword%) and u.userStatus <> 'DELETED' order by u.createdAt desc")
    Page<User> findAllByKeywordForAdmin(String keyword, Pageable pageable);

    @Query("SELECT u FROM User u where u.userStatus <> 'DELETED' order by u.createdAt desc")
    Page<User> findAllForAdmin(Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.point = u.point - :used WHERE u.id = :userId AND u.point >= :used")
    int deductPoints(@Param("userId") Long userId, @Param("used") long used);

    @Modifying
    @Query("UPDATE User u SET u.point = u.point + :earned WHERE u.id = :userId")
    void addPoints(@Param("userId") Long userId, @Param("earned") long earned);

}
