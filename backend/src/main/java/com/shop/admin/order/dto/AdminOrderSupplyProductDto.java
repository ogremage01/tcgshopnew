package com.shop.admin.order.dto;

import com.shop.order.dto.OrderProductDto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AdminOrderSupplyProductDto extends OrderProductDto {

    private String supplyType;
    private String maker;
    private Long totalStock;

}
