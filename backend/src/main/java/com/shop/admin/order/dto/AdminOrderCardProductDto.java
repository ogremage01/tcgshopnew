package com.shop.admin.order.dto;

import java.time.LocalDateTime;

import com.shop.order.dto.OrderProductDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderCardProductDto extends OrderProductDto {

    private String game;
    private String setCode;
    private String setName;
    private LocalDateTime releaseDate;
    private Long setNumber;
    private Long totalStock;
    private String memo;
    private String storageName;
    private String printType;
    private String printing;
    private String language;
    private String condition;

}
