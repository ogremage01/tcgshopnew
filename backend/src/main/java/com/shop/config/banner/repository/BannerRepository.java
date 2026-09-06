package com.shop.config.banner.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.config.banner.entity.Banner;
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderByDisplayOrderAsc();
    @Query("SELECT b FROM Banner b WHERE b.target = :target AND b.active = true ORDER BY b.displayOrder ASC")
    List<Banner> findByTargetOrderByDisplayOrderAsc(@Param("target") String target);

    @Modifying
    @Query("UPDATE Banner SET displayOrder = displayOrder + 1 WHERE target = :target")
    void updateDisplayOrderCauseAdd(@Param("target") String target);


    @Modifying
    @Query("UPDATE Banner SET active = false WHERE id = :id")
    void softDeleteById(@Param("id") Long id);
}