package com.shop.offline.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.offline.product.entity.OfflineProductReceivingHistory;

public interface OfflineProductReceivingHistoryRepository
        extends JpaRepository<OfflineProductReceivingHistory, Long> {

    @EntityGraph(attributePaths = "items")
    Page<OfflineProductReceivingHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<OfflineProductReceivingHistory> findWithItemsById(Long id);
}
