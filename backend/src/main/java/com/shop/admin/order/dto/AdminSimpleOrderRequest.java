package com.shop.admin.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.shop.common.page.dto.PageParam;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSimpleOrderRequest {
    private PageParam pageParam;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> orderStatusList;

}
