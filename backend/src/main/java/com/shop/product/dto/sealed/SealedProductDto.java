package com.shop.product.dto.sealed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SealedProductDto {

    private Long id;
    private String productNameEn;
    private String productNameKo;
    private String game;
    private String setName;
    private String setCode;
    private Long price;
    private Integer currentVisibleStock;
    private Integer totalStock;
    private Integer maxVisibleStock;
    private String imageUrl;
    private Boolean isActive;
    private Boolean isDeleted;
    private String publicId;
    private String language;
    private String offlineProductId;
}
