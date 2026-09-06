package com.shop.config.banner.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.common.fileUpload.service.FileUploadService;
import com.shop.config.banner.dto.AddBannerDto;
import com.shop.config.banner.dto.BannerDto;
import com.shop.config.banner.dto.ChangeBannerOrderDto;
import com.shop.config.banner.dto.SetBannerDto;
import com.shop.config.banner.dto.UpdateSetBannerDto;
import com.shop.config.banner.entity.Banner;
import com.shop.config.banner.entity.SetBanner;
import com.shop.config.banner.entity.SetBannerId;
import com.shop.config.banner.repository.BannerRepository;
import com.shop.config.banner.repository.SetBannerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {
    private final BannerRepository bannerRepository;
    private final SetBannerRepository setBannerRepository;
    private final FileUploadService fileUploadService;
    @Override
    public List<BannerDto> findAllByOrderByDisplayOrderAsc() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc().stream()
            .map(Banner::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<BannerDto> findTargetByOrderByDisplayOrderAsc(String target) {
        return bannerRepository.findByTargetOrderByDisplayOrderAsc(target).stream()
            .map(Banner::toDto)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addBanner(String target, AddBannerDto dto) throws IOException {
        String imageUrl = fileUploadService.uploadFile(dto.getImageFile(), "banners/" + target);

        bannerRepository.updateDisplayOrderCauseAdd(target);
        
        
        Banner banner = Banner.builder()
            .target(target)
            .imageUrl(imageUrl)
            .link(dto.getLink())
            .title(dto.getTitle())
            .displayOrder(1)
            .active(true)
            .build();
        bannerRepository.save(banner);
    }

    @Override
    @Transactional
    public void changeBannerOrder(String target, ChangeBannerOrderDto dto) {
        if (dto == null || dto.getOrderedIds() == null || dto.getOrderedIds().isEmpty()) {
            throw new RuntimeException("orderedIds is empty");
        }

        Map<Long, Integer> displayOrderById = new HashMap<>();
        for (int i = 0; i < dto.getOrderedIds().size(); i++) {
            displayOrderById.put(dto.getOrderedIds().get(i), i + 1);
        }

        List<Banner> banners = bannerRepository.findByTargetOrderByDisplayOrderAsc(target);

        for (Banner banner : banners) {
            Integer displayOrder = displayOrderById.get(banner.getId());
            if (displayOrder != null) {
                banner.setDisplayOrder(displayOrder);
            }
        }

        bannerRepository.saveAll(banners);
    }

    @Override
    @Transactional
    public void deleteBanner(Long id) {
        bannerRepository.softDeleteById(id);
    }

    @Override
    public List<SetBannerDto> findAllSetBanners() {
        return setBannerRepository.findAllByOrderById_GameAscId_BannerIdAsc().stream()
                .map(SetBanner::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SetBannerDto> findActiveSetBanner(String game, String bannerId) {
        Optional<SetBannerDto> result = setBannerRepository.findById(new SetBannerId(game, bannerId))
                .filter(banner -> Boolean.TRUE.equals(banner.getBannerActive()))
                .map(SetBanner::toDto);
        if (result.isPresent() || "default".equals(bannerId)) {
            log.info("findActiveSetBanner: {}", result);
            return result;
        }
        return setBannerRepository.findById(new SetBannerId(game, "default"))
                .filter(banner -> Boolean.TRUE.equals(banner.getBannerActive()))
                .map(SetBanner::toDto);
    }

    @Override
    @Transactional
    public void updateSetBanner(String game, String bannerId, UpdateSetBannerDto dto) throws IOException {
        SetBanner banner = setBannerRepository.findById(new SetBannerId(game, bannerId))
                .orElseThrow(() -> new RuntimeException("세트 배너를 찾을 수 없습니다."));

        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            String imageUrl = fileUploadService.uploadFile(dto.getImageFile(), "banners/set/" + game);
            banner.setBannerImageUrl(imageUrl);
        }
        if (dto.getLink() != null) {
            banner.setBannerLink(dto.getLink());
        }
        if (dto.getTitle() != null) {
            banner.setBannerTitle(dto.getTitle());
        }
        if (dto.getActive() != null) {
            banner.setBannerActive(dto.getActive());
        }

        setBannerRepository.save(banner);
    }
}
