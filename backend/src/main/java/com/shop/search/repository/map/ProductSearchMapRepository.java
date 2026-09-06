package com.shop.search.repository.map;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.ProductSearchMapRepositoryCustom;

public interface ProductSearchMapRepository
        extends JpaRepository<ProductSearchMap, Long>, ProductSearchMapRepositoryCustom {

    List<ProductSearchMap> findByProductNameContaining(String keyword);

    //ProductSearchMap findByProductId(String productId);

    Optional<ProductSearchMap> findBySourceIdAndTableName(Long sourceId, ProductTableEnum tableName);

    Optional<ProductSearchMap> findByProductIdAndTableName(String productId, ProductTableEnum tableName);
    List<ProductSearchMap> findByTableNameAndProductIdIn(ProductTableEnum tableName, List<String> productIds);
    List<ProductSearchMap> findByTableNameAndCatalogSourceIdIn(ProductTableEnum tableName, List<Long> catalogSourceIds);

    List<ProductSearchMap> findBySourcePublicId(String sourcePublicId);

    @Modifying
    @Query(value = """
        UPDATE product_search_maps pm
        JOIN card_product cp ON pm.product_id = cp.public_id
        SET pm.is_visible = false, pm.in_stock = false, pm.updated_at = NOW()
        WHERE cp.storage_id = :storageId
        """, nativeQuery = true)
    void updateByStorageIdAndIsVisible(@Param("storageId") Long storageId);

    @Modifying
    @Query(value = """
        UPDATE product_search_maps
        SET is_visible = false, in_stock = false, updated_at = NOW()
        WHERE table_name = 'CARD_PRODUCT'
          AND product_id IN (:publicIds)
        """, nativeQuery = true)
    void hideByCardProductPublicIds(@Param("publicIds") List<String> publicIds);

    @Modifying
    @Query(value = """
        UPDATE product_search_maps psm
        JOIN card_product cp ON cp.public_id = psm.product_id
        SET psm.is_visible = true,
            psm.in_stock = (COALESCE(cp.current_visible_stock, 0) > 0),
            psm.updated_at = NOW()
        WHERE psm.table_name = 'CARD_PRODUCT'
          AND psm.product_id IN (:publicIds)
        """, nativeQuery = true)
    void restoreByCardProductPublicIds(@Param("publicIds") List<String> publicIds);

    /**
     * UNION_PRICE 대표 행 in_stock을 연결된 visible CardProduct 재고 집계와 일치시킨다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE product_search_maps psm
        LEFT JOIN (
            SELECT cp.union_price_id
            FROM card_product cp
            WHERE cp.is_visible = TRUE
              AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
              AND COALESCE(cp.current_visible_stock, 0) > 0
            GROUP BY cp.union_price_id
        ) stocked ON stocked.union_price_id = psm.source_id
        SET psm.in_stock = (stocked.union_price_id IS NOT NULL),
            psm.updated_at = NOW()
        WHERE psm.table_name = 'UNION_PRICE'
        """, nativeQuery = true)
    int reconcileUnionPriceInStock();

}
