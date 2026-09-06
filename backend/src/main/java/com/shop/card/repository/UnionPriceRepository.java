package com.shop.card.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.dto.slim.UnionPriceSlimDto;
import com.shop.card.entity.UnionPrice;

public interface UnionPriceRepository extends JpaRepository<UnionPrice, Long> {

    List<UnionPrice> findByCheckCodeRefined(String checkCodeRefined);

    @Query("""
            SELECT u
            FROM UnionPrice u
            WHERE u.id > :lastId
              AND (u.publicId IS NULL OR TRIM(u.publicId) = '')
            ORDER BY u.id ASC
            """)
    List<UnionPrice> findPublicIdBackfillTargets(@Param("lastId") Long lastId, Pageable pageable);

    @Query(value = """
            SELECT id
            FROM union_prices
            WHERE id > :lastId
              AND (public_id IS NULL OR TRIM(public_id) = '')
            ORDER BY id ASC
            LIMIT 1000
            """, nativeQuery = true)
    List<Long> findPublicIdBackfillTargetIds(@Param("lastId") Long lastId, @Param("limit") int limit);

    List<UnionPrice> findAllByCheckCodeRefinedIn(Collection<String> checkCodeRefineds);

    // 카드 정보 검색
    @Query("""
            SELECT new com.shop.card.dto.slim.UnionPriceSlimDto(
                u.id, u.game, u.productType, u.printType, u.printing, u.setCode, u.setNumber, u.cardName, u.cardNameK, u.imageSource, u.imageUrl,
                u.checkCodeRefined, u.rarity, u.price
            ) FROM UnionPrice u
               WHERE u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                 OR u.cardName LIKE CONCAT('%', :keyword, '%')
                 OR u.cardNameK LIKE CONCAT('%', :keyword, '%')
            ORDER BY u.game ASC, u.setCode ASC,
                     CASE WHEN u.setNumber IS NULL THEN 1 ELSE 0 END ASC,
                     u.setNumber ASC, u.cardName ASC, u.cardNameK ASC
            """)
    Page<UnionPriceSlimDto> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 카드 정보 검색
    @Query("""
            SELECT new com.shop.card.dto.slim.UnionPriceSlimDto(
                u.id, u.game, u.productType, u.printType, u.printing, u.setCode, u.setNumber, u.cardName, u.cardNameK, u.imageSource, u.imageUrl,
                u.checkCodeRefined, u.rarity, u.price
            ) FROM UnionPrice u
               WHERE u.game = :game
                 AND (u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                 OR u.cardName LIKE CONCAT('%', :keyword, '%')
                 OR u.cardNameK LIKE CONCAT('%', :keyword, '%')
                 )
            ORDER BY u.setCode ASC,
                     CASE WHEN u.setNumber IS NULL THEN 1 ELSE 0 END ASC,
                     u.setNumber ASC, u.cardName ASC, u.cardNameK ASC
            """)
    Page<UnionPriceSlimDto> findByKeywordFilterByGame(@Param("game") String game, @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT new com.shop.card.dto.slim.UnionPriceSlimDto(
                u.id, u.game, u.productType, u.printType, u.printing, u.setCode, u.setNumber, u.cardName, u.cardNameK, u.imageSource, u.imageUrl,
                u.checkCodeRefined, u.rarity, u.price
            ) FROM UnionPrice u
               WHERE (:game IS NULL OR :game = '' OR u.game = :game)
                 AND (:setCode IS NULL OR :setCode = '' OR u.setCode = :setCode)
                 AND LOWER(TRIM(COALESCE(u.productType, ''))) NOT IN ('sealed products', 'sealedproducts')
                 AND (u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                 OR u.cardName LIKE CONCAT('%', :keyword, '%')
                 OR u.cardNameK LIKE CONCAT('%', :keyword, '%')
                 )
            ORDER BY u.game ASC, u.setCode ASC,
                     CASE WHEN u.setNumber IS NULL THEN 1 ELSE 0 END ASC,
                     u.setNumber ASC, u.cardName ASC, u.cardNameK ASC
            """)
    Page<UnionPriceSlimDto> findByKeywordWithFilters(
            @Param("keyword") String keyword,
            @Param("game") String game,
            @Param("setCode") String setCode,
            Pageable pageable);

    // 이미지 메타 업데이트
    @Query("""
            SELECT DISTINCT new com.shop.card.dto.slim.UnionPriceSearchFacetRowDto(
                u.game, u.setCode, u.setName
            ) FROM UnionPrice u
               WHERE (u.checkCodeRefined LIKE CONCAT('%', :keyword, '%')
                 OR u.cardName LIKE CONCAT('%', :keyword, '%')
                 OR u.cardNameK LIKE CONCAT('%', :keyword, '%')
                 )
                 AND LOWER(TRIM(COALESCE(u.productType, ''))) NOT IN ('sealed products', 'sealedproducts')
                 AND u.game IS NOT NULL
                 AND TRIM(u.game) <> ''
                 AND u.setCode IS NOT NULL
                 AND TRIM(u.setCode) <> ''
            ORDER BY u.game ASC, u.setCode ASC, u.setName ASC
            """)
    List<com.shop.card.dto.slim.UnionPriceSearchFacetRowDto> findGameSetFacetsByKeyword(
            @Param("keyword") String keyword);

    @Query("""
            SELECT DISTINCT new com.shop.card.dto.slim.UnionPriceSearchFacetRowDto(
                u.game, u.setCode, u.setName
            ) FROM UnionPrice u
               WHERE (u.cardName LIKE CONCAT('%', :keyword, '%')
                 OR u.cardNameK LIKE CONCAT('%', :keyword, '%')
                 )
                 AND LOWER(TRIM(COALESCE(u.productType, ''))) NOT IN ('sealed products', 'sealedproducts')
                 AND u.game IS NOT NULL
                 AND TRIM(u.game) <> ''
                 AND u.setCode IS NOT NULL
                 AND TRIM(u.setCode) <> ''
            ORDER BY u.game ASC, u.setCode ASC, u.setName ASC
            """)
    List<com.shop.card.dto.slim.UnionPriceSearchFacetRowDto> findGameSetFacetsByCardName(
            @Param("keyword") String keyword);

    @Query("""
            SELECT new com.shop.card.dto.slim.UnionPriceSlimDto(
                u.id, u.game, u.productType, u.printType, u.printing, u.setCode, u.setNumber, u.cardName, u.cardNameK, u.imageSource, u.imageUrl,
                u.checkCodeRefined, u.rarity, u.price
            ) FROM UnionPrice u
               WHERE u.setCode LIKE CONCAT('%', :setCode, '%')
                 AND u.setNumber = :setNumber
                 AND (:game IS NULL OR :game = '' OR u.game = :game)
                 AND LOWER(TRIM(COALESCE(u.productType, ''))) NOT IN ('sealed products', 'sealedproducts')
            ORDER BY u.game ASC, u.setCode ASC, u.setNumber ASC, u.cardName ASC, u.cardNameK ASC
            """)
    Page<UnionPriceSlimDto> findBySetCodeAndSetNumber(
            @Param("setCode") String setCode,
            @Param("setNumber") Long setNumber,
            @Param("game") String game,
            Pageable pageable);

    @Query("""
            SELECT new com.shop.card.dto.slim.UnionPriceSlimDto(
                u.id, u.game, u.productType, u.printType, u.printing, u.setCode, u.setNumber, u.cardName, u.cardNameK, u.imageSource, u.imageUrl,
                u.checkCodeRefined, u.rarity, u.price
            ) FROM UnionPrice u
               WHERE (:game IS NULL OR :game = '' OR u.game = :game)
                 AND (:setCode IS NULL OR :setCode = '' OR u.setCode = :setCode)
                 AND LOWER(TRIM(COALESCE(u.productType, ''))) NOT IN ('sealed products', 'sealedproducts')
                 AND (u.cardName LIKE CONCAT('%', :keyword, '%')
                 OR u.cardNameK LIKE CONCAT('%', :keyword, '%')
                 )
            ORDER BY u.game ASC, u.setCode ASC,
                     CASE WHEN u.setNumber IS NULL THEN 1 ELSE 0 END ASC,
                     u.setNumber ASC, u.cardName ASC, u.cardNameK ASC
            """)
    Page<UnionPriceSlimDto> findByCardNameWithFilters(
            @Param("keyword") String keyword,
            @Param("game") String game,
            @Param("setCode") String setCode,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE UnionPrice u
               SET u.imageSource = :imageSource,
                   u.imageUrl = :imageUrl,
                   u.isDoubleSided = :isDoubleSided
                WHERE u.checkCodeRefined = :checkCodeRefined
            """)
    int updateImageMetaByCheckCodeRefined(@Param("checkCodeRefined") String checkCodeRefined,
            @Param("imageSource") String imageSource,
            @Param("imageUrl") String imageUrl,
            @Param("isDoubleSided") Boolean isDoubleSided);

    // 게임 + 세트 코드 또는 세트 이름(동일 값으로 OR 매칭). 엑셀은 setCode만 넘겨도 조회 가능.
    @Query("""
            SELECT u
            FROM UnionPrice u
            WHERE u.game = :game
              AND (u.setCode = :setCodeOrName OR u.setName = :setCodeOrName)
              AND LOWER(u.printType) LIKE CONCAT('%', LOWER(:printType), '%')
              AND u.productType = 'Cards'
              ORDER BY u.setNumber ASC
            """)

    List<UnionPrice> findForExcelExport(@Param("game") String game,
            @Param("setCodeOrName") String setCodeOrName,
            @Param("printType") String printType);

    @Query("""
            SELECT DISTINCT u.rarity
            FROM UnionPrice u
            WHERE u.id IN :ids
              AND u.rarity IS NOT NULL
              AND TRIM(u.rarity) <> ''
            ORDER BY u.rarity ASC
            """)
    List<String> findDistinctRaritiesByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT DISTINCT u.setName
            FROM UnionPrice u
            WHERE u.id IN :ids
              AND u.setName IS NOT NULL
              AND TRIM(u.setName) <> ''
            ORDER BY u.setName ASC
            """)
    List<String> findDistinctSetNamesByIds(@Param("ids") Collection<Long> ids);

    @Query("""
            SELECT u
            FROM UnionPrice u
            WHERE u.cardName = :cardName
              AND u.id = :unionPriceId
            """)
    UnionPrice findByCardName(@Param("cardName") String cardName, @Param("unionPriceId") Long unionPriceId);

    @Query("""
            SELECT u
            FROM UnionPrice u
            WHERE u.cardNameK = :cardNameK
              AND u.id = :unionPriceId
            """)
    UnionPrice findByCardNameK(@Param("cardNameK") String cardNameK, @Param("unionPriceId") Long unionPriceId);

}
