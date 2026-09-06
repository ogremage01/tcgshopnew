package com.shop.product.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import com.shop.product.metadata.entity.TcgPProductLine;

public interface TcgPProductLineRepository extends JpaRepository<TcgPProductLine, Long> {
    Optional<TcgPProductLine> findByProductLineId(Long productLineId);
}
