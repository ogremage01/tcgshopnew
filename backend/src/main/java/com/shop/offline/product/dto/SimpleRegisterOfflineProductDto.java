package com.shop.offline.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleRegisterOfflineProductDto {
    private Long offlineProductId;
    private String linkTableName;
}
