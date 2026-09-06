package com.shop.log.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.shop.log.order.entity.OrderLog;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLogDto {

    private Long id;

    private Long orderId;

    private String category;

    private String action;

    private LocalDateTime actionDate;

    private Long userId;

    private String userName;

    private Boolean isSuccess;

    public OrderLog toEntity() {
        return new OrderLog(id, orderId, category, action, actionDate, userId, userName, isSuccess);
    }
}
