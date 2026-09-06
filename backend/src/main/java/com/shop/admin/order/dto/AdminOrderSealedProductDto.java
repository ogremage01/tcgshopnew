package com.shop.admin.order.dto;

import com.shop.order.dto.OrderProductDto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AdminOrderSealedProductDto extends OrderProductDto {

    private String game;
    private String language;
    private Long totalStock;

}
