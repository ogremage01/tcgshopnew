package com.shop.product.repository.manualProduct;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.shop.product.entity.manualProduct.ManualProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualProductRepository extends JpaRepository<ManualProduct, Long> {

    Optional<ManualProduct> findById(Long id);

    @Query("SELECT m FROM ManualProduct m WHERE (m.isDeleted = false OR m.isDeleted IS NULL)")
    Page<ManualProduct> findAllForAdmin(Pageable pageable);

    @Query("SELECT m FROM ManualProduct m WHERE (m.isDeleted = false OR m.isDeleted IS NULL) AND (m.nameKo LIKE %:keyword% OR m.nameEn LIKE %:keyword%)")
    Page<ManualProduct> findByKeywordForAdmin(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 재고 차감: 조건 불만족 시 영향 행 0.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE manual_products
            SET stock = stock - :qty
            WHERE id = :id
              AND stock >= :qty
              AND is_visible = true
              AND (is_deleted = false OR is_deleted IS NULL)
            """, nativeQuery = true)
    int deductStockIfAvailable(@Param("id") Long id, @Param("qty") long qty);

    /** 주문 취소 시 재고 복구 */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE manual_products
            SET stock = stock + :qty
            WHERE id = :id
            """, nativeQuery = true)
    int restoreStock(@Param("id") Long id, @Param("qty") long qty);

}