package com.shop.product.repository.supply;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.product.entity.supplies.Supply;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

    @Query("""
            SELECT s FROM Supply s
            WHERE (s.isDeleted = false OR s.isDeleted IS NULL)
            ORDER BY s.id DESC
            """)
    Page<Supply> findAllForAdmin(Pageable pageable);

    @Query("""
            SELECT s FROM Supply s
            WHERE (s.isDeleted = false OR s.isDeleted IS NULL)
              AND (
                LOWER(s.nameKo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.maker) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(s.supplyType) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY s.id DESC
            """)
    Page<Supply> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 재고 차감: 조건 불만족 시 영향 행 0.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE supplies
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
            UPDATE supplies
            SET stock = stock + :qty
            WHERE id = :id
            """, nativeQuery = true)
    int restoreStock(@Param("id") Long id, @Param("qty") long qty);
}
