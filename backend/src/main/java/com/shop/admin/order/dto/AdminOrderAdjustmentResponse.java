package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderAdjustmentResponse {

    /** PG 환불 성공 여부 */
    private boolean pgRefundSuccess;

    /** 실패 시 사유 메시지 (성공이면 null) */
    private String pgRefundMessage;

    /** 주문이 취소 상태로 전환되었는지 */
    private boolean orderCancelled;

    /** 장바구니 복구가 1건 이상 적용되었는지 */
    private boolean cartRestoreApplied;

    private int cartLinesFullyRestored;

    private int cartLinesPartiallyRestored;

    private int cartLinesSkipped;
}
