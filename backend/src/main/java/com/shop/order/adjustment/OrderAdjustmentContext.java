package com.shop.order.adjustment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.shop.admin.order.dto.AdminOrderProductModifyRequest;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderAdjustmentContext {

    private OrderAdjustmentType type;
    private Long orderId;
    private boolean restoreStockOnCancel;
    private boolean restoreCartOnCancel;
    private AdminOrderProductModifyRequest modifyRequest;

    /** FULL_CANCEL 시 관리자 입력 취소(환불) 사유 */
    private String cancelReason;

    private OrderInfo orderInfo;

    @Builder.Default
    private BigDecimal originalUsedPointAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal originalActualPaymentAmount = BigDecimal.ZERO;

    @Builder.Default
    private long originalEarnedPointAmount = 0L;

    @Builder.Default
    private BigDecimal newUsedPointAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal newActualPaymentAmount = BigDecimal.ZERO;

    @Builder.Default
    private long newEarnedPointAmount = 0L;

    @Builder.Default
    private long releasedUsedPoints = 0L;

    @Builder.Default
    private BigDecimal pgRefundAmount = BigDecimal.ZERO;

    @Builder.Default
    private long earnedPointDelta = 0L;

    @Builder.Default
    private List<OrderProduct> remainingLines = new ArrayList<>();

    private Long snapshotId;

    @Builder.Default
    private List<StockRestoreEntry> stockRestoreEntries = new ArrayList<>();

    @Builder.Default
    private boolean pgRefundSuccess = true;

    private String pgRefundMessage;

    @Builder.Default
    private boolean orderCancelled = false;

    @Builder.Default
    private boolean cartRestoreRequested = false;

    @Builder.Default
    private boolean cartRestoreApplied = false;

    @Builder.Default
    private int cartLinesRequested = 0;

    @Builder.Default
    private int cartLinesFullyRestored = 0;

    @Builder.Default
    private int cartLinesPartiallyRestored = 0;

    @Builder.Default
    private int cartLinesSkipped = 0;
}
