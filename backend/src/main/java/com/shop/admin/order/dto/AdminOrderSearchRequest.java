package com.shop.admin.order.dto;

import com.shop.common.page.dto.PageParam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderSearchRequest {
    private PageParam pageParam;
    private String keyword;
}
