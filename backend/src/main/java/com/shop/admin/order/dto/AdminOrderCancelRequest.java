package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderCancelRequest {
    /** true면 주문 라인별 재고를 복구한다. */
    private boolean restoreStock;
    /** true면 회원 주문의 취소 상품을 장바구니에 복구한다. */
    private boolean restoreCart;
    /** 취소(환불) 사유. Toss cancelReason으로 전달되며 필수. */
    private String cancelReason;
}
