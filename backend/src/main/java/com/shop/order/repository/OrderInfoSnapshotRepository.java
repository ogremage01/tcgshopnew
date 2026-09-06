package com.shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.order.entity.OrderInfoSnapshot;

public interface OrderInfoSnapshotRepository extends JpaRepository<OrderInfoSnapshot, Long> {
}
