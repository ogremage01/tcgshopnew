package com.shop.admin.product.dto.card;

import java.util.List;

import org.springframework.data.domain.Page;

import com.shop.card.dto.slim.UnionPriceGameSetFacetDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardProductAdminSearchResponseDto {
    private Page<CardProductManagementResponseDto> cards;
    private List<UnionPriceGameSetFacetDto> facets;
}
