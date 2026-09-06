package com.shop.admin.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderProductModifyItem {

    /** 수정 대상 OrderProduct ID */
    private Long orderProductId;

    /** 변경할 수량 (null이면 수량 유지, 감소만 허용) */
    private Long newQuantity;

    /** true면 해당 라인 삭제 */
    private boolean deleted;

    /** true면 재고 복구 (삭제 또는 수량 감소 시 체크해야 복구됨) */
    private boolean restoreStock;
}
