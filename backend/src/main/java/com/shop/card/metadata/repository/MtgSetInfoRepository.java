package com.shop.card.metadata.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.card.metadata.entity.MtgSetInfo;

public interface MtgSetInfoRepository extends JpaRepository<MtgSetInfo, Long> {

    List<MtgSetInfo> findAllByOrderByNameAsc();

    List<MtgSetInfo> findAllByOrderByReleaseDateDesc();

    @Query("SELECT m FROM MtgSetInfo m WHERE LOWER(m.type) NOT IN ('Tokens', 'memorabilia') ORDER BY m.name ASC")
    List<MtgSetInfo> findAllByOrderByNameAscOnlyCardSets();

    @Query("SELECT m FROM MtgSetInfo m WHERE LOWER(m.type) NOT IN ('Tokens', 'memorabilia') ORDER BY m.releaseDate DESC")
    List<MtgSetInfo> findAllByOrderByReleaseDateDescOnlyCardSets();

    //드롭다운 메뉴 표시용. 포함하고 싶은 세트를 입력하시오.
    @Query("SELECT m FROM MtgSetInfo m WHERE LOWER(m.type) IN ('expansion', 'masters') AND m.releaseDate <= :releaseDate ORDER BY m.releaseDate DESC")
    List<MtgSetInfo> findAllByReleaseDateBeforeOrderByReleaseDateDesc(LocalDateTime releaseDate);

    List<MtgSetInfo> findAllByOrderBySetCodeAsc();

    boolean existsBySetCode(String setCode);

    Optional<MtgSetInfo> findBySetCode(String setCode);
}
