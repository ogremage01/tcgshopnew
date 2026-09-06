package com.shop.card.metadata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.card.metadata.entity.FabSetInfo;

public interface FabSetInfoRepository extends JpaRepository<FabSetInfo, Long> {

    boolean existsBySetCode(String setCode);

    Optional<FabSetInfo> findBySetCode(String setCode);
    List<FabSetInfo> findBySetCodeIn(List<String> setCodes);
    List<FabSetInfo> findAllByOrderByPorderAsc();
    List<FabSetInfo> findAllByOrderByPorderDesc();
    @Query("SELECT f FROM FabSetInfo f WHERE f.category = 'Booster' ORDER BY f.porder DESC")
    List<FabSetInfo> mainHeaderNavSetsByCategoryOrderByPorderDesc();
}
