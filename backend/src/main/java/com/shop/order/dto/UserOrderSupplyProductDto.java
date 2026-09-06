package com.shop.order.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserOrderSupplyProductDto extends OrderProductDto {

    private String supplyType;
    private String maker;

}
