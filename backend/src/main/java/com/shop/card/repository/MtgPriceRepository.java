package com.shop.card.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.entity.MtgPrice;

import java.util.List;
import java.util.Optional;

public interface MtgPriceRepository extends JpaRepository<MtgPrice, Long> {

    // mtg-kr MTG 가격 리포지토리

    Optional<MtgPrice> findBySetAndCodeAndType(String set, String code, String type);

    List<MtgPrice> findByTcgPPriceIdIsNull();

    List<MtgPrice> findByCheckCodeIsNotNull();

    /** checkCode 목록으로 한 번에 조회 - mergeMtgWithExisting의 N+1 방지용 */
    List<MtgPrice> findByCheckCodeIn(List<String> checkCodes);

    @Query("SELECT p FROM MtgPrice p WHERE p.id > :lastId AND (p.downloaded = false OR p.downloaded IS NULL) ORDER BY p.id ASC")
    List<MtgPrice> findOpenBinderImageDownloadTargets(@Param("lastId") Long lastId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE MtgPrice p SET p.checkCode = NULL, p.checkCodeRefined = NULL")
    void resetCheckCodes();

    @Modifying
    @Transactional
    @Query("UPDATE MtgPrice p SET p.tcgPPriceId = NULL")
    void resetLinks();
}
