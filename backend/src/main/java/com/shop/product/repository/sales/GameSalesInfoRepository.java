package com.shop.product.repository.sales;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.product.entity.card.GameSalesInfo;

public interface GameSalesInfoRepository extends JpaRepository<GameSalesInfo, Long> {

    Optional<GameSalesInfo> findByGame(String game);
}
