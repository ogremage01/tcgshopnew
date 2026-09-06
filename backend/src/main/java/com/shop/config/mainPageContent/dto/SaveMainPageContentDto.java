package com.shop.config.mainPageContent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveMainPageContentDto {
    private String name;
    private String content;
    private String link;
}
