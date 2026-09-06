package com.shop.card.dto.slim;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnionPriceAdminSearchResponseDto {
    private Page<UnionPriceSlimDto> cards;
    private List<UnionPriceGameSetFacetDto> facets;
}
