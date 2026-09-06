package com.shop.reward.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.reward.entity.RewardRule;

public interface RewardRuleRepository extends JpaRepository<RewardRule, Long> {

    @Query("SELECT r FROM RewardRule r WHERE r.isActive = true " +
           "AND (r.startAt IS NULL OR r.startAt <= :now) " +
           "AND (r.endAt IS NULL OR r.endAt >= :now) " +
           "ORDER BY r.rank ASC")
    List<RewardRule> findActiveOrderedByRank(@Param("now") LocalDateTime now);

    List<RewardRule> findAllByOrderByRankAsc();

    @Query("SELECT COALESCE(MAX(r.rank), 0) FROM RewardRule r")
    Integer findMaxRank();

    @Modifying
    @Query("UPDATE RewardRule r SET r.rank = :rank WHERE r.id = :id")
    void updateRank(@Param("id") Long id, @Param("rank") Integer rank);
}
