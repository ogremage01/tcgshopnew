package com.shop.config.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerDto {
    private Long id;
    private String target;
    private String imageUrl;
    private String link;
    private String title;
    private Integer displayOrder;
}
