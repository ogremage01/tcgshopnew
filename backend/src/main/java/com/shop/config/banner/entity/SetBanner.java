package com.shop.config.banner.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.shop.config.banner.dto.SetBannerDto;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "set_banners")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetBanner {

    @EmbeddedId
    private SetBannerId id;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "banner_link", length = 500)
    private String bannerLink;

    @Column(name = "banner_title", length = 255)
    private String bannerTitle;

    @Column(name = "banner_active")
    private Boolean bannerActive;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public SetBannerDto toDto() {
        return SetBannerDto.builder()
                .game(id.getGame())
                .bannerId(id.getBannerId())
                .imageUrl(bannerImageUrl)
                .link(bannerLink)
                .title(bannerTitle)
                .active(bannerActive)
                .build();
    }
}
