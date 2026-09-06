package com.shop.product.dto.manual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualProductDto {
    private Long id;
    private String nameEn;
    private String nameKo;
    /** ProductCategory 마스터 nameEn (저장값) */
    private String productType;
    private String categoryNameEn;
    private String categoryNameKo;
    private String description;
    private Long price;
    private Long stock;
    /** ProductIp 마스터 nameEn (저장값) */
    private String productIp;
    private String productIpNameEn;
    private String productIpNameKo;
    private String imgUrl;
    private Boolean isDeleted;
    private Boolean isVisible;
    private String publicId;
    private String offlineProductId;
}
