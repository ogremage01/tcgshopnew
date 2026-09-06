package com.shop.config.mainHeader.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainHeaderDto {
    private Long id;
    private String title;
    private String urlString;
    private Boolean isActive;
    private Integer displayOrder;
}
