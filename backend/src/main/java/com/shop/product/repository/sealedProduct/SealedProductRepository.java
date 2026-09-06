package com.shop.product.repository.sealedProduct;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.product.entity.sealedProduct.SealedProduct;

public interface SealedProductRepository extends JpaRepository<SealedProduct, Long> {

    Optional<SealedProduct> findByPublicId(String publicId);

    @Query("""
            SELECT p FROM SealedProduct p
            WHERE (p.isDeleted IS NULL OR p.isDeleted = false)
              AND (:keyword IS NULL OR LOWER(p.productNameKo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                    OR LOWER(p.productNameEn) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:game IS NULL OR p.game = :game)
              AND (:setCode IS NULL OR p.setCode = :setCode)
              AND (:language IS NULL OR LOWER(p.language) = LOWER(:language))
            """)
    Page<SealedProduct> findByFilters(
            @Param("keyword") String keyword,
            @Param("game") String game,
            @Param("setCode") String setCode,
            @Param("language") String language,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT p.game, p.setCode, p.setName
            FROM SealedProduct p
            WHERE (p.isDeleted IS NULL OR p.isDeleted = false)
              AND p.game IS NOT NULL
            ORDER BY p.game, p.setCode
            """)
    List<Object[]> findDistinctGameSetCodes();

    @Query("""
            SELECT DISTINCT p.setName
            FROM SealedProduct p
            WHERE p.id IN :ids
              AND p.setName IS NOT NULL
              AND TRIM(p.setName) <> ''
            ORDER BY p.setName ASC
            """)
    List<String> findDistinctSetNamesByIds(@Param("ids") Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE sealed_product
            SET current_visible_stock = current_visible_stock - :qty,
                total_stock = total_stock - :qty
            WHERE id = :id
              AND current_visible_stock >= :qty
              AND total_stock >= :qty
              AND is_active = true
              AND (is_deleted = false OR is_deleted IS NULL)
            """, nativeQuery = true)
    int deductStockIfAvailable(@Param("id") Long id, @Param("qty") long qty);

    /** 주문 취소 시 재고 복구 (current_visible_stock은 max_visible_stock을 초과할 수 없음) */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE sealed_product
            SET total_stock = total_stock + :qty,
                current_visible_stock = LEAST(current_visible_stock + :qty, max_visible_stock)
            WHERE id = :id
            """, nativeQuery = true)
    int restoreStock(@Param("id") Long id, @Param("qty") long qty);
}
