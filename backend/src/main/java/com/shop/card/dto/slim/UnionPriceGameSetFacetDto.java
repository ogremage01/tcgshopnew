package com.shop.card.dto.slim;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnionPriceGameSetFacetDto {
    private String game;
    private List<UnionPriceSetFacetDto> sets;
}
