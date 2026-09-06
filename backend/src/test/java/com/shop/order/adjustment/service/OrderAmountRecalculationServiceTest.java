package com.shop.order.adjustment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderAmountRecalculationServiceTest {

    @Mock
    private OrderProductRepository orderProductRepository;
    @Mock
    private OrderInfoRepository orderInfoRepository;
    @InjectMocks
    private OrderAmountRecalculationService service;

    @Test
    @DisplayName("usedPoint cap 시 releasedUsedPoints와 pgRefundAmount를 분리 계산한다")
    void capsUsedPointsAndComputesDiffs() {
        OrderInfo orderInfo = OrderInfo.builder()
                .id(1L)
                .deliveryFee(BigDecimal.ZERO)
                .usedPointAmount(BigDecimal.valueOf(5000))
                .build();

        OrderProduct line = OrderProduct.builder()
                .totalPrice(BigDecimal.valueOf(3000))
                .quantity(1L)
                .rewardPoints(50L)
                .build();

        when(orderProductRepository.findByOrderInfoId(1L)).thenReturn(List.of(line));

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderId(1L)
                .orderInfo(orderInfo)
                .originalUsedPointAmount(BigDecimal.valueOf(5000))
                .originalActualPaymentAmount(BigDecimal.valueOf(10000))
                .originalEarnedPointAmount(100L)
                .build();

        service.recalculate(ctx);

        assertThat(ctx.getNewUsedPointAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(ctx.getReleasedUsedPoints()).isEqualTo(2000L);
        assertThat(ctx.getPgRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(ctx.getEarnedPointDelta()).isEqualTo(50L);
    }

    @Test
    @DisplayName("FULL_CANCEL 시 original 기준 전액 diff를 기록한다")
    void fullCancelUsesOriginalAmounts() {
        OrderInfo orderInfo = OrderInfo.builder()
                .id(2L)
                .deliveryFee(BigDecimal.valueOf(3000))
                .usedPointAmount(BigDecimal.valueOf(1000))
                .actualPaymentAmount(BigDecimal.valueOf(9000))
                .earnedPointAmount(200L)
                .build();

        when(orderProductRepository.findByOrderInfoId(2L)).thenReturn(List.of());

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderId(2L)
                .orderInfo(orderInfo)
                .originalUsedPointAmount(BigDecimal.valueOf(1000))
                .originalActualPaymentAmount(BigDecimal.valueOf(9000))
                .originalEarnedPointAmount(200L)
                .build();

        service.recalculate(ctx);

        assertThat(ctx.getReleasedUsedPoints()).isEqualTo(1000L);
        assertThat(ctx.getPgRefundAmount()).isEqualByComparingTo(BigDecimal.valueOf(9000));
        assertThat(ctx.getEarnedPointDelta()).isEqualTo(200L);
        assertThat(orderInfo.getActualPaymentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
