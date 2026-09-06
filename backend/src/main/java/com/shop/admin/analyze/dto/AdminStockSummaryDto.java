package com.shop.admin.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStockSummaryDto {

    //게임 이름
    private String gameName;
    //재고 합
    private long totalStockCount;
    //재고 금액 합
    private long totalStockAmount;

}
