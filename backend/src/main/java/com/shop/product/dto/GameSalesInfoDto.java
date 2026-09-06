package com.shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSalesInfoDto {

    private String game;
    private String company;
    private String brand;
    private String origin;
    private String recommendedAge;

}
