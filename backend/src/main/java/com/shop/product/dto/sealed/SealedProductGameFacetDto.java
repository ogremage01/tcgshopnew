package com.shop.product.dto.sealed;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SealedProductGameFacetDto {

    private String game;
    private List<SetItemDto> sets;

    @Data
    @AllArgsConstructor
    public static class SetItemDto {
        private String setCode;
        private String setName;
    }
}
