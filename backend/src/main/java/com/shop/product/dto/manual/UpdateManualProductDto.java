package com.shop.product.dto.manual;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateManualProductDto {

    private String nameEn;
    private String nameKo;
    /** ProductCategory 마스터 nameEn */
    private String productType;
    private String description;
    private Long price;
    private Long stock;
    /** ProductIp 마스터 nameEn */
    private String productIp;
    private MultipartFile imageFile;
    private Boolean isVisible;
    private String offlineProductId;
}
