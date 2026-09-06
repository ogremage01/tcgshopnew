package com.shop.product.dto.card;

import com.shop.product.dto.ProductItemDto;
import com.shop.product.entity.card.GameSalesInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualProductDto {

    private ProductItemDto productItemDto;
    private GameSalesInfo gameSalesInfo;

}
