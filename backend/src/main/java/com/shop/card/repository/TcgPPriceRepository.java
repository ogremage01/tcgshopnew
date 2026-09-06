package com.shop.card.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.entity.TcgPPrice;
import com.shop.product.dto.card.slim.TcgPPriceCardSlimDto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TcgPPriceRepository extends JpaRepository<TcgPPrice, Long> {

    // TCGPlayer 가격 리포지토리

    List<TcgPPrice> findByProductIdIn(Collection<Long> productIds);

    // 상품 이름으로 검색하여 카드 정보 조회
    @Query("""
                SELECT new com.shop.product.dto.card.slim.TcgPPriceCardSlimDto(
                    p.id, p.productId, p.game, p.productName, p.type, p.rarity, p.marketPrice, p.isDoubleSided,p.setAbbrv, p.printType, p.printing, p.condition)
                FROM TcgPPrice p
                WHERE p.productName LIKE CONCAT('%', :productName, '%')
            """)
    List<TcgPPriceCardSlimDto> findByProductName(@Param("productName") String productName);

    // 상품 이름혹은 코드넘버로 검색하여 카드 정보 조회
    @Query("""
                SELECT new com.shop.product.dto.card.slim.TcgPPriceCardSlimDto(
                    p.id, p.productId, p.game, p.productName, p.type,
                    p.rarity, p.marketPrice, p.isDoubleSided,
                    p.setAbbrv, p.printType, p.printing, p.condition
                )
                FROM TcgPPrice p
                WHERE p.productName LIKE CONCAT('%', :keyword, '%')
                   OR p.codeNumber LIKE CONCAT('%', :keyword, '%')
                ORDER BY p.id DESC
            """)
    Page<TcgPPriceCardSlimDto> findByProductNameOrCodeNumber(
            @Param("keyword") String keyword,
            Pageable pageable);

    // 등록 상품 조회 (동일 productId 행이 여러 개일 수 있음)
    List<TcgPPrice> findAllByProductId(Long productId);

    Optional<TcgPPrice> findByProductId(Long productId);

    @Query("SELECT DISTINCT p FROM TcgPPrice p WHERE p.downloaded = false OR p.downloaded IS NULL")
    List<TcgPPrice> findAllCardImageDownloadTargets();

    // 게임과 세트명으로 카드 조회
    @Query("SELECT p FROM TcgPPrice p WHERE p.game = :game AND p.set = :set ORDER BY p.number ASC")
    List<TcgPPrice> findByGameAndSetNameForExcelExport(@Param("game") String game, @Param("set") String set);

    List<TcgPPrice> findByGame(String game);

    // 상품 이름혹은 코드넘버로 검색하여 카드 정보 조회
    @Query("""
                SELECT new com.shop.product.dto.card.slim.TcgPPriceCardSlimDto(
                    p.id, p.productId, p.game, p.productName, p.type,
                    p.rarity, p.marketPrice, p.isDoubleSided,
                    p.setAbbrv, p.printType, p.printing, p.condition
                )
                FROM TcgPPrice p
                WHERE p.productName LIKE CONCAT('%', :keyword, '%')
                   OR p.codeNumber LIKE CONCAT('%', :keyword, '%')
            """)
    List<TcgPPriceCardSlimDto> findByProductNameOrCodeNumberList(
            @Param("keyword") String keyword);

    @Modifying
    @Transactional
    @Query("UPDATE TcgPPrice p SET p.checkCode = NULL, p.checkCodeRefined = NULL WHERE p.game IN :games")
    void resetCheckCodes(@Param("games") List<String> games);

    @Modifying
    @Transactional
    @Query("UPDATE TcgPPrice p SET p.obPriceId = NULL WHERE p.game IN :games")
    void resetLinks(@Param("games") List<String> games);
}
