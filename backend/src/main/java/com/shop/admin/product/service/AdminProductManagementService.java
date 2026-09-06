package com.shop.admin.product.service;

import com.shop.product.service.CardProductService;
import com.shop.product.dto.card.slim.TcgPPriceCardSlimDto;
import com.shop.admin.product.dto.SingleProductSearchResultData;
import com.shop.admin.product.dto.card.CardProductManagementResponseDto;
import com.shop.product.dto.card.management.CardProductManagementDto;
import com.shop.card.service.TcgPPriceService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductManagementService {

        private final CardProductService cardProductService;
        private final TcgPPriceService tcgPPriceService;

        public SingleProductSearchResultData searchProducts(String keyword, Pageable pageable) {
                Page<TcgPPriceCardSlimDto> cards = tcgPPriceService.findByProductNameOrCodeNumber(keyword, pageable);
                List<Long> cardIdList = tcgPPriceService.findByProductNameOrCodeNumberList(keyword).stream()
                                .map(TcgPPriceCardSlimDto::getId).collect(Collectors.toList());
                Page<CardProductManagementDto> products = cardProductService.findByProductIdInForAdmin(
                                cardIdList,
                                pageable);
                Page<CardProductManagementResponseDto> response = products.map(CardProductManagementResponseDto::from);
                return SingleProductSearchResultData.builder()
                                .productList(response)
                                .tcgPcardList(cards)
                                .build();
        }
}
