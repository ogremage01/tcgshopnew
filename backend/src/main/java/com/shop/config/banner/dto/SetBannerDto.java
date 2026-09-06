package com.shop.config.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetBannerDto {
    private String game;
    private String bannerId;
    private String imageUrl;
    private String link;
    private String title;
    private Boolean active;
}
