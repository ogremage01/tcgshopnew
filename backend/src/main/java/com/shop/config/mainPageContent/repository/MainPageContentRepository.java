package com.shop.config.mainPageContent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.config.mainPageContent.entity.MainPageContent;

public interface MainPageContentRepository extends JpaRepository<MainPageContent, Long> {

    List<MainPageContent> findAllByActiveTrueOrderByDisplayOrderAsc();

    @Modifying
    @Query("UPDATE MainPageContent m SET m.displayOrder = m.displayOrder + 1 WHERE m.active = true")
    void incrementAllDisplayOrders();

    @Modifying
    @Query("UPDATE MainPageContent m SET m.active = false WHERE m.id = :id")
    void softDeleteById(@Param("id") Long id);
}
