package com.shop.product.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.product.metadata.entity.TcgPSyncGame;

import java.util.List;
import java.util.Optional;

public interface TcgPSyncGameRepository extends JpaRepository<TcgPSyncGame, Long> {
    Optional<TcgPSyncGame> findByProductLineName(String productLineName);

    List<TcgPSyncGame> findByProductLineNameIn(List<String> productLineNames);

    @Query("select g.productLineId from TcgPSyncGame g")
    List<Long> findAllProductLineIds();

    Optional<TcgPSyncGame> findByProductLineId(Long productLineId);
}
