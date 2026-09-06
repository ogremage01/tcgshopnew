package com.shop.config.mainPageContent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainPageContentDto {
    private Long id;
    private String name;
    private String content;
    private String imageUrl;
    private String link;
    private Integer displayOrder;
}
