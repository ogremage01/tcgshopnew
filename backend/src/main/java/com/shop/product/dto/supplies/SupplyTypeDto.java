package com.shop.product.dto.supplies;

import com.shop.product.entity.supplies.SupplyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplyTypeDto {

    private Long id;
    private String nameEn;
    private String nameKo;

    public SupplyType toEntity() {
        return SupplyType.builder()
                .id(id)
                .nameEn(nameEn)
                .nameKo(nameKo)
                .build();
    }
}
