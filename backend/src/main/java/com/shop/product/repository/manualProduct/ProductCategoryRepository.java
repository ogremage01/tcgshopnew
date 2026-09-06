package com.shop.product.repository.manualProduct;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.product.entity.manualProduct.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    @Query("SELECT c FROM ProductCategory c WHERE c.isDeleted = :isDeleted")
    List<ProductCategory> findAllByIsDeleted(Boolean isDeleted);

    @Query("SELECT c FROM ProductCategory c WHERE c.isDeleted = :isDeleted")
    Page<ProductCategory> findAllByIsDeleted(Boolean isDeleted, Pageable pageable);

    Optional<ProductCategory> findByNameEn(String nameEn);
}
