package com.shop.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.shop.common.page.dto.PageParam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 관리자 주문 목록 등에서 사용하는 {@link com.shop.order.entity.OrderInfo} 조회 조건 (리포지토리 공용). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInfoListCriteria {

    private PageParam pageParam;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> orderStatusList;
}
