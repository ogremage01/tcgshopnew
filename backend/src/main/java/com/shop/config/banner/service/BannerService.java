package com.shop.config.banner.service;

import java.io.IOException;
import java.util.List;

import java.util.Optional;

import com.shop.config.banner.dto.AddBannerDto;
import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.dto.ChangeBannerOrderDto;
import com.shop.config.banner.dto.SetBannerDto;
import com.shop.config.banner.dto.UpdateSetBannerDto;

public interface BannerService {
    void addBanner(String target, AddBannerDto dto) throws IOException;
    void changeBannerOrder(String target, ChangeBannerOrderDto dto);
    List<BannerDto> findAllByOrderByDisplayOrderAsc();
    List<BannerDto> findTargetByOrderByDisplayOrderAsc(String target);
    void deleteBanner(Long id);

    List<SetBannerDto> findAllSetBanners();
    Optional<SetBannerDto> findActiveSetBanner(String game, String bannerId);
    void updateSetBanner(String game, String bannerId, UpdateSetBannerDto dto) throws IOException;
}
