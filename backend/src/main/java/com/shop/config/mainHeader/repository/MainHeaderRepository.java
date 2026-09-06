package com.shop.config.mainHeader.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.shop.config.mainHeader.entity.MainHeader;

public interface MainHeaderRepository extends JpaRepository<MainHeader, Long> {

    List<MainHeader> findAllByOrderByDisplayOrderAsc();

    List<MainHeader> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    @Modifying
    @Query("UPDATE MainHeader m SET m.displayOrder = m.displayOrder + 1")
    void incrementAllDisplayOrders();
}
