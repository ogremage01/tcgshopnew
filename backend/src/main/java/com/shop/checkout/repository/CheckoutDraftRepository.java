package com.shop.checkout.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.checkout.domain.DraftStatus;
import com.shop.checkout.entity.CheckoutDraft;

import jakarta.persistence.LockModeType;

public interface CheckoutDraftRepository extends JpaRepository<CheckoutDraft, Long> {

    Optional<CheckoutDraft> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM CheckoutDraft d WHERE d.publicId = :publicId")
    Optional<CheckoutDraft> findByPublicIdForUpdate(@Param("publicId") String publicId);

    List<CheckoutDraft> findByGuestIdAndStatus(String guestId, DraftStatus status);

    List<CheckoutDraft> findByUserIdAndStatus(Long userId, DraftStatus status);

    List<CheckoutDraft> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE CheckoutDraft d SET d.status = :newStatus WHERE d.guestId = :guestId AND d.status = :oldStatus")
    int updateStatusByGuestIdAndStatus(@Param("guestId") String guestId,
            @Param("oldStatus") DraftStatus oldStatus,
            @Param("newStatus") DraftStatus newStatus);

    @Modifying
    @Query("UPDATE CheckoutDraft d SET d.status = :newStatus WHERE d.userId = :userId AND d.status = :oldStatus")
    int updateStatusByUserIdAndStatus(@Param("userId") Long userId,
            @Param("oldStatus") DraftStatus oldStatus,
            @Param("newStatus") DraftStatus newStatus);
}
