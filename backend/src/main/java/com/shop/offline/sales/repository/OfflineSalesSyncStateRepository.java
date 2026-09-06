package com.shop.offline.sales.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.offline.sales.entity.OfflineSalesSyncState;

public interface OfflineSalesSyncStateRepository extends JpaRepository<OfflineSalesSyncState, Long> {

    Optional<OfflineSalesSyncState> findById(Long id);
}
