package com.shop.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.product.entity.ProductIp;

public interface ProductIpRepository extends JpaRepository<ProductIp, Long> {

    @Query("SELECT p FROM ProductIp p WHERE p.isDeleted = :isDeleted")
    List<ProductIp> findAllByIsDeleted(Boolean isDeleted);

    @Query("SELECT p FROM ProductIp p WHERE p.isDeleted = :isDeleted")
    Page<ProductIp> findAllByIsDeleted(Boolean isDeleted, Pageable pageable);

    Optional<ProductIp> findByNameEn(String nameEn);
}
