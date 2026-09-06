package com.shop.admin.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOfflineSalesTotalDto {
    private String category;
    private String item;
    private Long quantity;
    private Long amount;
}
