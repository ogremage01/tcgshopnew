package com.shop.product.repository.supply;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.product.entity.supplies.SupplyType;

public interface SupplyTypeRepository extends JpaRepository<SupplyType, Long> {

    @Query("SELECT s FROM SupplyType s WHERE s.isDeleted = :isDeleted")
    List<SupplyType> findAllByIsDeleted(Boolean isDeleted);

    @Query("SELECT s FROM SupplyType s WHERE s.isDeleted = :isDeleted")
    Page<SupplyType> findAllByIsDeleted(Boolean isDeleted, Pageable pageable);

    Optional<SupplyType> findByNameEn(String nameEn);
    Optional<SupplyType> findByNameKo(String nameKo);

    List<SupplyType> findByNameKoIn(Collection<String> nameKos);

    List<SupplyType> findByNameEnIn(Collection<String> nameEns);
}
