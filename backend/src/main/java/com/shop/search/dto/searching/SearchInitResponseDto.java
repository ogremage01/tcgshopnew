package com.shop.search.dto.searching;

import java.util.List;

import org.springframework.data.domain.Page;

import com.shop.product.dto.ProductItemDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 검색 초기 응답 DTO
 * 
 * @author 오우람
 * @since 2026-04-24
 */
public class SearchInitResponseDto {
    private Page<ProductItemDto> products;
    private List<String> games;
    private List<String> productTypes;
    private List<SuppliesTypeFacetDto> suppliesTypes;
    private List<String> manualCategories;
    private List<String> printTypes;
    private List<String> rarities;
    private List<String> setNames;
}
