package com.shop.order.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** 고객용 주문 상세 — 카드 라인(재고·보관 위치·메모 미포함). */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderCardProductDto extends OrderProductDto {

    private String game;
    private String setCode;
    private String setName;
    private LocalDateTime releaseDate;
    private Long setNumber;
    private String printType;
    private String printing;
    private String language;
    private String condition;
}
