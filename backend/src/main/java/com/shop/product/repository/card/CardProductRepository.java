package com.shop.product.repository.card;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.admin.analyze.dto.AdminStockSummaryDto;
import com.shop.card.dto.slim.UnionPriceSearchFacetRowDto;
import com.shop.product.entity.card.CardProduct;

public interface CardProductRepository extends JpaRepository<CardProduct, Long> {

    // 카드 제품 리포지토리

    // 외부 노출용 ULID로 카드 제품 조회
    Optional<CardProduct> findByPublicId(String publicId);

    @Query("SELECT c FROM CardProduct c WHERE c.isVisible = true AND c.isDeleted = false AND c.id IN :productIds")
    Page<CardProduct> findByProductIdInVisibleAndIsNotDeleted(List<Long> productIds, Pageable pageable);

    @Query(value = """
            SELECT c FROM CardProduct c
            JOIN FETCH c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (:game IS NULL OR :game = '' OR u.game = :game)
              AND (:setCode IS NULL OR :setCode = '' OR u.setCode = :setCode)
              AND (u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                OR u.cardName LIKE CONCAT('%', :keyword, '%')
                OR u.cardNameK LIKE CONCAT('%', :keyword, '%'))
            """, countQuery = """
            SELECT COUNT(c) FROM CardProduct c
            JOIN c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (:game IS NULL OR :game = '' OR u.game = :game)
              AND (:setCode IS NULL OR :setCode = '' OR u.setCode = :setCode)
              AND (u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                OR u.cardName LIKE CONCAT('%', :keyword, '%')
                OR u.cardNameK LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<CardProduct> findByKeywordForAdmin(
            @Param("keyword") String keyword,
            @Param("game") String game,
            @Param("setCode") String setCode,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT new com.shop.card.dto.slim.UnionPriceSearchFacetRowDto(
                u.game, u.setCode, u.setName
            )
            FROM CardProduct c
            JOIN c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                OR u.cardName LIKE CONCAT('%', :keyword, '%')
                OR u.cardNameK LIKE CONCAT('%', :keyword, '%'))
              AND u.game IS NOT NULL
              AND TRIM(u.game) <> ''
              AND u.setCode IS NOT NULL
              AND TRIM(u.setCode) <> ''
            ORDER BY u.game ASC, u.setCode ASC, u.setName ASC
            """)
    List<UnionPriceSearchFacetRowDto> findGameSetFacetsByKeywordForAdmin(@Param("keyword") String keyword);

    @Query(value = """
            SELECT c FROM CardProduct c
            JOIN FETCH c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND u.setCode LIKE CONCAT('%', :setCode, '%')
              AND u.setNumber = :setNumber
              AND (:game IS NULL OR :game = '' OR u.game = :game)
            """, countQuery = """
            SELECT COUNT(c) FROM CardProduct c
            JOIN c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND u.setCode LIKE CONCAT('%', :setCode, '%')
              AND u.setNumber = :setNumber
              AND (:game IS NULL OR :game = '' OR u.game = :game)
            """)
    Page<CardProduct> findBySetCodeAndSetNumberForAdmin(
            @Param("setCode") String setCode,
            @Param("setNumber") long setNumber,
            @Param("game") String game,
            Pageable pageable);

    @Query(value = """
            SELECT c FROM CardProduct c
            JOIN FETCH c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (:game IS NULL OR :game = '' OR u.game = :game)
              AND (:setCode IS NULL OR :setCode = '' OR u.setCode = :setCode)
              AND (u.cardName LIKE CONCAT('%', :keyword, '%')
                OR u.cardNameK LIKE CONCAT('%', :keyword, '%'))
            """, countQuery = """
            SELECT COUNT(c) FROM CardProduct c
            JOIN c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (:game IS NULL OR :game = '' OR u.game = :game)
              AND (:setCode IS NULL OR :setCode = '' OR u.setCode = :setCode)
              AND (u.cardName LIKE CONCAT('%', :keyword, '%')
                OR u.cardNameK LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<CardProduct> findByCardNameForAdmin(
            @Param("keyword") String keyword,
            @Param("game") String game,
            @Param("setCode") String setCode,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT new com.shop.card.dto.slim.UnionPriceSearchFacetRowDto(
                u.game, u.setCode, u.setName
            )
            FROM CardProduct c
            JOIN c.unionPrice u
            WHERE (c.isDeleted = false OR c.isDeleted IS NULL)
              AND (u.cardName LIKE CONCAT('%', :keyword, '%')
                OR u.cardNameK LIKE CONCAT('%', :keyword, '%'))
              AND u.game IS NOT NULL
              AND TRIM(u.game) <> ''
              AND u.setCode IS NOT NULL
              AND TRIM(u.setCode) <> ''
            ORDER BY u.game ASC, u.setCode ASC, u.setName ASC
            """)
    List<UnionPriceSearchFacetRowDto> findGameSetFacetsByCardNameForAdmin(@Param("keyword") String keyword);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE card_product
            SET current_visible_stock = LEAST(max_visible_stock, total_stock)
            WHERE is_deleted = false
              AND is_visible = true
              AND is_auto_updated_stock = true
            """, nativeQuery = true)
    int syncStock();

    @Query("""
            SELECT c FROM CardProduct c
            JOIN FETCH c.unionPrice u
            WHERE c.isVisible = true
              AND (c.isDeleted = false OR c.isDeleted IS NULL)
              AND c.isAutoUpdatedStock = true
            """)
    List<CardProduct> findAutoUpdatedVisibleWithUnionPrice();

    @Query("SELECT c FROM CardProduct c WHERE c.unionPrice.id = :unionPriceId")
    Page<CardProduct> findByUnionPriceIdForAdmin(@Param("unionPriceId") Long unionPriceId, Pageable pageable);

    @Query("SELECT c FROM CardProduct c WHERE c.isVisible = true AND c.isDeleted = false AND c.id IN :productIds")
    Page<CardProduct> findByProductIdInForAdmin(List<Long> productIds, Pageable pageable);

    @Query("""
            SELECT c FROM CardProduct c
            WHERE c.unionPrice.id = :unionPriceId
              AND c.isVisible = :isVisible
              AND c.isDeleted = :isDeleted
              AND c.storageId = :storageId
              AND c.isPriceLinked = :isPriceLinked
              AND c.pricingRate = :pricingRate
              AND c.language = :language
              AND c.condition = :condition
              AND c.printType = :printType""")
    Optional<CardProduct> findByPattern(@Param("unionPriceId") Long unionPriceId, @Param("isVisible") Boolean isVisible,
            @Param("isDeleted") Boolean isDeleted, @Param("storageId") Long storageId,
            @Param("isPriceLinked") Boolean isPriceLinked, @Param("pricingRate") Double pricingRate,
            @Param("language") String language, @Param("condition") String condition,
            @Param("printType") String printType);

    @Modifying
    @Query(value = """
            UPDATE card_product
            SET is_deleted = true,
                memo = CONCAT('삭제된 상품-보관소 삭제(', :storageId, ')', IFNULL(memo, ''))
            WHERE storage_id = :storageId
            """, nativeQuery = true)
    int updateByStorageIdAndIsDeleted(@Param("storageId") Long storageId);

    @Modifying
    @Query(value = """
            UPDATE card_product
            SET is_deleted = true, is_visible = false,
                memo = CONCAT('삭제된 상품-단건 삭제(', :id, ')', IFNULL(memo, ''))
            WHERE id = :id
            """, nativeQuery = true)
    int softDeleteCardProductById(@Param("id") Long id);

    @Query("SELECT c FROM CardProduct c WHERE c.isPriceLinked = :isPriceLinked")
    List<CardProduct> findByIsPriceLinked(@Param("isPriceLinked") Boolean isPriceLinked);

    @Query("SELECT c FROM CardProduct c WHERE c.isPriceLinked = true")
    Page<CardProduct> findPageByIsPriceLinkedTrue(Pageable pageable);

    @Query("SELECT c FROM CardProduct c WHERE c.isPriceLinked = true AND c.condition = :condition")
    Page<CardProduct> findPageByIsPriceLinkedTrueAndCondition(@Param("condition") String condition, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE card_product cp
            INNER JOIN union_prices up ON cp.union_price_id = up.id
            INNER JOIN grade_pricing_policies gpp ON cp.card_condition = gpp.grade
            INNER JOIN price_config pc_rate
                ON pc_rate.config_key = 'us_currency_rate' AND pc_rate.config_game = up.game
            INNER JOIN price_config pc_min
                ON pc_min.config_key = 'minimum_price' AND pc_min.config_game = up.game
            SET cp.calculated_linked_price = GREATEST(
                pc_min.config_value,
                CEILING(up.price * COALESCE(cp.pricing_rate, 1) * gpp.percentage * pc_rate.config_value / 100) * 100
            )
            WHERE cp.is_price_linked = true
              AND up.price IS NOT NULL
            """, nativeQuery = true)
    int bulkUpdateCalculatedLinkedPrice();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE card_product cp
            INNER JOIN union_prices up ON cp.union_price_id = up.id
            INNER JOIN grade_pricing_policies gpp ON cp.card_condition = gpp.grade
            INNER JOIN price_config pc_rate
                ON pc_rate.config_key = 'us_currency_rate' AND pc_rate.config_game = up.game
            INNER JOIN price_config pc_min
                ON pc_min.config_key = 'minimum_price' AND pc_min.config_game = up.game
            SET cp.calculated_linked_price = GREATEST(
                pc_min.config_value,
                CEILING(up.price * COALESCE(cp.pricing_rate, 1) * gpp.percentage * pc_rate.config_value / 100) * 100
            )
            WHERE cp.is_price_linked = true
              AND cp.card_condition = :grade
              AND up.price IS NOT NULL
            """, nativeQuery = true)
    int bulkUpdateCalculatedLinkedPriceByGrade(@Param("grade") String grade);

    @Query("SELECT c FROM CardProduct c WHERE c.storageId = :storageId")
    List<CardProduct> findByStorageId(@Param("storageId") Long storageId);

    @Modifying
    @Query(value = """
            UPDATE card_product cp
            INNER JOIN union_prices up ON cp.union_price_id = up.id
            SET cp.price = :minimumPrice
            WHERE cp.is_price_linked = true
              AND cp.price < :minimumPrice
              AND up.game = :game
            """, nativeQuery = true)
    int raiseMinimumPriceForCardProducts(@Param("minimumPrice") Long minimumPrice,
            @Param("game") String game);

    @Modifying
    @Query(value = """
            UPDATE card_product cp
            INNER JOIN union_prices up ON cp.union_price_id = up.id
            SET cp.price = :newMinimumPrice
            WHERE cp.is_price_linked = true
              AND cp.price = :lastMinimumPrice
              AND up.game = :game
            """, nativeQuery = true)
    int dropMinimumPriceForCardProducts(@Param("newMinimumPrice") Long newMinimumPrice,
            @Param("lastMinimumPrice") Long lastMinimumPrice,
            @Param("game") String game);

    @Query("SELECT c FROM CardProduct c WHERE c.isVisible = true AND c.isDeleted = false AND c.id IN :productIds")
    List<CardProduct> findByProductIdInVisibleAndIsNotDeleted(List<Long> productIds);

    @Query("SELECT c FROM CardProduct c JOIN FETCH c.unionPrice u WHERE c.isVisible = true AND c.isDeleted = false AND u.id IN :unionPriceIds")
    List<CardProduct> findVisibleWithUnionPriceByUnionPriceIds(@Param("unionPriceIds") List<Long> unionPriceIds);

    // ---- 가격 오류 관리 ----

    @Query(value = """
            SELECT cp.* FROM card_product cp
            JOIN union_prices up ON cp.union_price_id = up.id
            WHERE up.price = 0
              AND cp.is_price_linked = true
              AND (cp.is_deleted = false OR cp.is_deleted IS NULL)
            """,
            countQuery = """
            SELECT COUNT(*) FROM card_product cp
            JOIN union_prices up ON cp.union_price_id = up.id
            WHERE up.price = 0
              AND cp.is_price_linked = true
              AND (cp.is_deleted = false OR cp.is_deleted IS NULL)
            """,
            nativeQuery = true)
    Page<CardProduct> findPriceErrorCards(Pageable pageable);

    @Query(value = """
            SELECT cp.public_id FROM card_product cp
            JOIN union_prices up ON cp.union_price_id = up.id
            WHERE up.price = 0
              AND cp.is_price_linked = true
              AND cp.is_visible = true
              AND (cp.is_deleted = false OR cp.is_deleted IS NULL)
            """, nativeQuery = true)
    List<String> findPublicIdsByZeroPriceAndVisible();

    @Query(value = """
            SELECT cp.public_id FROM card_product cp
            JOIN union_prices up ON cp.union_price_id = up.id
            WHERE cp.hidden_by_price_error = true
              AND (up.price > 0 OR cp.is_price_linked = false OR cp.is_price_linked IS NULL)
              AND (cp.is_deleted = false OR cp.is_deleted IS NULL)
            """, nativeQuery = true)
    List<String> findPublicIdsByNonZeroPriceAndHiddenByError();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE card_product cp
            JOIN union_prices up ON cp.union_price_id = up.id
            SET cp.is_visible = false, cp.hidden_by_price_error = true
            WHERE up.price = 0
              AND cp.is_price_linked = true
              AND cp.is_visible = true
              AND (cp.is_deleted = false OR cp.is_deleted IS NULL)
            """, nativeQuery = true)
    int bulkHideZeroPriceLinkedProducts();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE card_product cp
            JOIN union_prices up ON cp.union_price_id = up.id
            SET cp.is_visible = true, cp.hidden_by_price_error = false
            WHERE cp.hidden_by_price_error = true
              AND (up.price > 0 OR cp.is_price_linked = false OR cp.is_price_linked IS NULL)
              AND (cp.is_deleted = false OR cp.is_deleted IS NULL)
            """, nativeQuery = true)
    int bulkRestoreNonZeroPriceLinkedProducts();

    @Query("SELECT c FROM CardProduct c JOIN FETCH c.unionPrice u WHERE c.id = :id")
    Optional<CardProduct> findByIdWithUnionPrice(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM CardProduct c JOIN FETCH c.unionPrice u WHERE c.id IN :ids")
    List<CardProduct> findByIdInWithUnionPrice(@Param("ids") List<Long> ids);

    /**
     * 재고 차감: 조건 불만족 시 영향 행 0.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE card_product
            SET current_visible_stock = current_visible_stock - :qty, total_stock = total_stock - :qty
            WHERE id = :id
              AND current_visible_stock >= :qty
              AND is_visible = true
              AND (is_deleted = false OR is_deleted IS NULL)
            """, nativeQuery = true)
    int deductStockIfAvailable(@Param("id") Long id, @Param("qty") long qty);

    /** 주문 취소 시 재고 복구 (current_visible_stock은 max_visible_stock을 초과할 수 없음) */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE card_product
            SET total_stock = total_stock + :qty,
                current_visible_stock = LEAST(current_visible_stock + :qty, max_visible_stock)
            WHERE id = :id
            """, nativeQuery = true)
    int restoreStock(@Param("id") Long id, @Param("qty") long qty);

    @Query("""
        SELECT new com.shop.admin.analyze.dto.AdminStockSummaryDto(
            u.game,
            SUM(cp.totalStock),
            SUM(cp.totalStock * CASE WHEN cp.isPriceLinked = true THEN cp.calculatedLinkedPrice ELSE cp.price END)
        )
        FROM CardProduct cp JOIN cp.unionPrice u
        WHERE cp.isDeleted = false GROUP BY u.game
        """)
    List<AdminStockSummaryDto> groupByGameNameAndSumTotalStockCountAndSumTotalStockAmount();
}
