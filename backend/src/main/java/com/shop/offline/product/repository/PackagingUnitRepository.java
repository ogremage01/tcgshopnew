package com.shop.offline.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.offline.product.entity.PackagingUnit;

public interface PackagingUnitRepository extends JpaRepository<PackagingUnit, Long> {

    Optional<PackagingUnit> findByPieceId(Long pieceId);

    Optional<PackagingUnit> findByPackagingId(Long packagingId);

    boolean existsByPieceId(Long pieceId);

    boolean existsByPackagingId(Long packagingId);

    boolean existsByPieceIdAndIdNot(Long pieceId, Long id);

    boolean existsByPackagingIdAndIdNot(Long packagingId, Long id);
}
