package com.shop.product.metadata.repository;

import com.shop.product.metadata.entity.TcgPProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TcgPProductTypeRepository extends JpaRepository<TcgPProductType, Long> {
    Optional<TcgPProductType> findByProductTypeId(Long productTypeId);
}
