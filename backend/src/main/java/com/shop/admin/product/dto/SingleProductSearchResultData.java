package com.shop.admin.product.dto;

import com.shop.admin.product.dto.card.CardProductManagementResponseDto;
import com.shop.product.dto.card.slim.TcgPPriceCardSlimDto;
import com.shop.product.dto.card.slim.MtgPriceSlimDto;
import com.shop.product.dto.card.slim.FabPriceSlimDto;

import org.springframework.data.domain.Page;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleProductSearchResultData {

    private Page<CardProductManagementResponseDto> productList;
    private Page<TcgPPriceCardSlimDto> tcgPcardList;
    private Page<MtgPriceSlimDto> cardKingdomCardList;
    private Page<FabPriceSlimDto> starcityCardList;
}
