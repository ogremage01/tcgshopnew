package com.shop.offline.product.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.offline.product.entity.OfflineProduct;

public interface OfflineProductRepository extends JpaRepository<OfflineProduct, Long> {

    Optional<OfflineProduct> findByProductId(String productId);

    Optional<OfflineProduct> findByLinkTableNameAndLinkId(String linkTableName, Long linkId);

    List<OfflineProduct> findAllByTitleIn(Collection<String> titles);

    @Query("""
            SELECT p
            FROM OfflineProduct p
            WHERE (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%'))
            ORDER BY
                CASE WHEN :sortBy = 'id' AND :sortDirection = 'asc' THEN p.id END ASC,
                CASE WHEN :sortBy = 'id' AND :sortDirection = 'desc' THEN p.id END DESC,
                CASE WHEN :sortBy = 'productId' AND :sortDirection = 'asc' THEN p.productId END ASC,
                CASE WHEN :sortBy = 'productId' AND :sortDirection = 'desc' THEN p.productId END DESC,
                CASE WHEN :sortBy = 'categoryTitle' AND :sortDirection = 'asc' THEN p.categoryTitle END ASC,
                CASE WHEN :sortBy = 'categoryTitle' AND :sortDirection = 'desc' THEN p.categoryTitle END DESC,
                CASE WHEN :sortBy = 'title' AND :sortDirection = 'asc' THEN p.title END ASC,
                CASE WHEN :sortBy = 'title' AND :sortDirection = 'desc' THEN p.title END DESC,
                CASE WHEN :sortBy = 'priceValue' AND :sortDirection = 'asc' THEN p.priceValue END ASC,
                CASE WHEN :sortBy = 'priceValue' AND :sortDirection = 'desc' THEN p.priceValue END DESC,
                CASE WHEN :sortBy = 'receivingQuantity' AND :sortDirection = 'asc'
                    THEN COALESCE(p.receivingQuantity, 0) END ASC,
                CASE WHEN :sortBy = 'receivingQuantity' AND :sortDirection = 'desc'
                    THEN COALESCE(p.receivingQuantity, 0) END DESC,
                CASE WHEN :sortBy = 'shippingQuantity' AND :sortDirection = 'asc'
                    THEN COALESCE(p.shippingQuantity, 0) END ASC,
                CASE WHEN :sortBy = 'shippingQuantity' AND :sortDirection = 'desc'
                    THEN COALESCE(p.shippingQuantity, 0) END DESC,
                CASE WHEN :sortBy = 'stockQuantity' AND :sortDirection = 'asc'
                    THEN COALESCE(p.receivingQuantity, 0) - COALESCE(p.shippingQuantity, 0) END ASC,
                CASE WHEN :sortBy = 'stockQuantity' AND :sortDirection = 'desc'
                    THEN COALESCE(p.receivingQuantity, 0) - COALESCE(p.shippingQuantity, 0) END DESC,
                CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'asc' THEN p.createdAt END ASC,
                CASE WHEN :sortBy = 'createdAt' AND :sortDirection = 'desc' THEN p.createdAt END DESC,
                CASE WHEN :sortBy = 'updatedAt' AND :sortDirection = 'asc' THEN p.updatedAt END ASC,
                CASE WHEN :sortBy = 'updatedAt' AND :sortDirection = 'desc' THEN p.updatedAt END DESC,
                CASE WHEN :sortBy = 'linkTableName' AND :sortDirection = 'asc'
                    THEN CASE WHEN p.linkTableName IS NULL THEN 0 ELSE 1 END END ASC,
                CASE WHEN :sortBy = 'linkTableName' AND :sortDirection = 'desc'
                    THEN CASE WHEN p.linkTableName IS NULL THEN 0 ELSE 1 END END DESC,
                CASE WHEN p.linkTableName IS NULL THEN 0 ELSE 1 END ASC,
                p.id DESC
            """)
    Page<OfflineProduct> findSorted(
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            Pageable pageable);

}
