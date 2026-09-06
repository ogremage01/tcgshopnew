package com.shop.order.adjustment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.admin.order.dto.AdminOrderAdjustmentResponse;
import com.shop.order.adjustment.step.AmountRecalcStep;
import com.shop.order.adjustment.step.CartRestoreStep;
import com.shop.order.adjustment.step.LineMutationStep;
import com.shop.order.adjustment.step.PaymentRefundStep;
import com.shop.order.adjustment.step.PointAdjustStep;
import com.shop.order.adjustment.step.SnapshotStep;
import com.shop.order.adjustment.step.StatusUpdateStep;
import com.shop.order.adjustment.step.StockRestoreStep;
import com.shop.order.adjustment.step.ValidateStep;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderAdjustmentOrchestrator {

    private final ValidateStep validateStep;
    private final SnapshotStep snapshotStep;
    private final LineMutationStep lineMutationStep;
    private final StockRestoreStep stockRestoreStep;
    private final CartRestoreStep cartRestoreStep;
    private final AmountRecalcStep amountRecalcStep;
    private final PointAdjustStep pointAdjustStep;
    private final PaymentRefundStep paymentRefundStep;
    private final StatusUpdateStep statusUpdateStep;

    @Transactional
    public AdminOrderAdjustmentResponse executeFullCancel(OrderAdjustmentCommand command) {
        OrderAdjustmentContext ctx = buildContext(command);
        runPipeline(ctx);
        return buildResponse(ctx);
    }

    @Transactional
    public AdminOrderAdjustmentResponse executePartialModify(OrderAdjustmentCommand command) {
        OrderAdjustmentContext ctx = buildContext(command);
        runPipeline(ctx);
        return buildResponse(ctx);
    }

    private AdminOrderAdjustmentResponse buildResponse(OrderAdjustmentContext ctx) {
        return AdminOrderAdjustmentResponse.builder()
                .pgRefundSuccess(ctx.isPgRefundSuccess())
                .pgRefundMessage(ctx.getPgRefundMessage())
                .orderCancelled(ctx.isOrderCancelled())
                .cartRestoreApplied(ctx.isCartRestoreApplied())
                .cartLinesFullyRestored(ctx.getCartLinesFullyRestored())
                .cartLinesPartiallyRestored(ctx.getCartLinesPartiallyRestored())
                .cartLinesSkipped(ctx.getCartLinesSkipped())
                .build();
    }

    private OrderAdjustmentContext buildContext(OrderAdjustmentCommand command) {
        return OrderAdjustmentContext.builder()
                .type(command.getType())
                .orderId(command.getOrderId())
                .restoreStockOnCancel(command.isRestoreStockOnCancel())
                .restoreCartOnCancel(command.isRestoreCartOnCancel())
                .modifyRequest(command.getModifyRequest())
                .cancelReason(command.getCancelReason())
                .build();
    }

    private void runPipeline(OrderAdjustmentContext ctx) {
        validateStep.run(ctx);
        if (ctx.getType() == OrderAdjustmentType.FULL_CANCEL) {
            paymentRefundStep.run(ctx);
        }
        snapshotStep.run(ctx);
        lineMutationStep.run(ctx);
        stockRestoreStep.run(ctx);
        cartRestoreStep.run(ctx);
        amountRecalcStep.run(ctx);
        pointAdjustStep.run(ctx);
        if (ctx.getType() != OrderAdjustmentType.FULL_CANCEL) {
            paymentRefundStep.run(ctx);
        }
        statusUpdateStep.run(ctx);
    }
}
