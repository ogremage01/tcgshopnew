package com.shop.admin.order.dto;

import com.shop.order.dto.OrderInfoDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminOrderInfoDto extends OrderInfoDto {

    /** 회원 주문일 때 관리자용 회원 메모 (유저 API에는 노출하지 않음). */
    private String userMemo;
}
