package com.shop.product.dto.supplies;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyDto {

    private Long id;
    private String publicId;
    private String nameEn;
    private String nameKo;
    private String description;
    private Long price;
    private Long stock;
    private String supplyType;
    private String maker;
    private Long makerId;
    private Long supplyTypeId;
    private String imgUrl;
    private Boolean isVisible;
    private Boolean isDeleted;
    private String offlineProductId;
}
