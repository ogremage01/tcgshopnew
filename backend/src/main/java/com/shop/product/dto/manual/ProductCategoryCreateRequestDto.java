package com.shop.product.dto.manual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryCreateRequestDto {
    private String nameEn;
    private String nameKo;
}
