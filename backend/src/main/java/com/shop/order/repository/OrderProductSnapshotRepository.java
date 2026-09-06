package com.shop.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.order.entity.OrderProductSnapshot;

public interface OrderProductSnapshotRepository extends JpaRepository<OrderProductSnapshot, Long> {
}
