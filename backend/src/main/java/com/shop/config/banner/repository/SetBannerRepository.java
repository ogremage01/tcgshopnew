package com.shop.config.banner.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.config.banner.entity.SetBanner;
import com.shop.config.banner.entity.SetBannerId;

public interface SetBannerRepository extends JpaRepository<SetBanner, SetBannerId> {

    List<SetBanner> findAllByOrderById_GameAscId_BannerIdAsc();
}
