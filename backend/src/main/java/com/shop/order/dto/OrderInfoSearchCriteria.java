package com.shop.order.dto;

import com.shop.common.page.dto.PageParam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 관리자 주문 키워드 검색 조건 (리포지토리 공용). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInfoSearchCriteria {

    private PageParam pageParam;
    private String keyword;
}
