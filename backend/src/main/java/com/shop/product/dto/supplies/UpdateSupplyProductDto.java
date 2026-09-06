package com.shop.product.dto.supplies;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSupplyProductDto {

    private String nameEn;
    private String nameKo;
    private String description;
    private Long price;
    private Long stock;
    private Long makerId;
    private Long supplyTypeId;
    private MultipartFile imageFile;
    private Boolean isVisible;
    private String offlineProductId;
}
