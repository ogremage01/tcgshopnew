package com.shop.card.dto.slim;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnionPriceSearchFacetRowDto {
    private String game;
    private String setCode;
    private String setName;
}
