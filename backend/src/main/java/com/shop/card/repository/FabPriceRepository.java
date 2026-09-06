package com.shop.card.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.entity.FabPrice;

import java.util.List;
import java.util.Optional;

public interface FabPriceRepository extends JpaRepository<FabPrice, Long> {

    // mtg-kr FAB 가격 리포지토리

    Optional<FabPrice> findBySetAndCode(String set, String code);

    List<FabPrice> findByTcgPPriceIdIsNull();

    List<FabPrice> findByCheckCodeIsNotNull();

    /** set 목록으로 한 번에 조회 - mergeFabWithExisting의 N+1 방지용 */
    List<FabPrice> findBySetIn(List<String> sets);

    @Query("SELECT p FROM FabPrice p WHERE p.id > :lastId AND (p.downloaded = false OR p.downloaded IS NULL) ORDER BY p.id ASC")
    List<FabPrice> findOpenBinderImageDownloadTargets(@Param("lastId") Long lastId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE FabPrice p SET p.checkCode = NULL, p.checkCodeRefined = NULL")
    void resetCheckCodes();

    @Modifying
    @Transactional
    @Query("UPDATE FabPrice p SET p.tcgPPriceId = NULL")
    void resetLinks();
}
