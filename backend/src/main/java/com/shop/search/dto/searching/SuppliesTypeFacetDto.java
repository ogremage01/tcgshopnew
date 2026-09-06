package com.shop.search.dto.searching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuppliesTypeFacetDto {
    private String nameEn;
    private String nameKo;
}
