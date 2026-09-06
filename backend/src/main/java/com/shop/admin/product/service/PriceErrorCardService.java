package com.shop.admin.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.admin.product.dto.card.CardProductManagementResponseDto;

public interface PriceErrorCardService {

    /**
     * 가격 연동(isPriceLinked=true)이면서 UnionPrice.price=0인 카드를 비공개로,
     * 가격이 정상화되었거나 수동 가격으로 전환된 카드를 재공개로 처리한다.
     * 가격 ingestion 완료 후 호출된다.
     */
    void syncPriceErrorVisibility();

    Page<CardProductManagementResponseDto> getPriceErrorCards(Pageable pageable);
}
