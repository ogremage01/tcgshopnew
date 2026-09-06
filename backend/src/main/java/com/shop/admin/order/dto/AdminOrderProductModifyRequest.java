package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderProductModifyRequest {

    private List<AdminOrderProductModifyItem> items;

    /** 취소(환불) 사유. Toss cancelReason으로 전달되며 필수. */
    private String cancelReason;
}
