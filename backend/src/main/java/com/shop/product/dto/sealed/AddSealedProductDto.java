package com.shop.product.dto.sealed;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddSealedProductDto {

    private String productNameEn;
    private String productNameKo;
    private String game;
    private String setName;
    private String setCode;
    private Long price;
    private Integer currentVisibleStock;
    private Integer totalStock;
    private Integer maxVisibleStock;
    private MultipartFile imageFile;
    private String language;
    private String offlineProductId;
    private Boolean isActive;
}
