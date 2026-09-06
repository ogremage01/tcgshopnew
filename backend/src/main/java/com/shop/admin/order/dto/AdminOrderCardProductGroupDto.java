package com.shop.admin.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 관리자 주문 상세 — 카드 라인을 게임 단위로 묶은 그룹. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderCardProductGroupDto {

    /** {@link com.shop.search.entity.ProductSearchMap#getGame()} 값 (예: {@code GameEnum.MTG#getGame()}). */
    private String game;

    private List<AdminOrderCardProductDto> products;
}
