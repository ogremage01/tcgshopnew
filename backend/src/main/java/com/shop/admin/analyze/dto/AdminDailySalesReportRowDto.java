package com.shop.admin.analyze.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDailySalesReportRowDto {

    /**
     * 매출 일자
     */
    private LocalDate orderDate;

    /**
     * 총 매출(온라인(DIRECT 제외 상품 + 배송료) + 오프라인(싱글카드 포함)).
     * DIRECT는 오프라인에도 잡히므로 총합에서만 제외한다.
     */
    private long totalAmount;

    /**
     * 온라인 매출(order_products 합 + 배송료, payment_method=DIRECT 포함)
     */
    private long onlineAmount;

    /**
     * 오프라인 매출(오프라인 아이템 금액, 싱글카드 포함)
     */
    private long offlineAmount;
}
