package com.shop.product.dto.supplies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyTypeCreateRequestDto {
    private String nameEn;
    private String nameKo;
}
