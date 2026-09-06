package com.shop.order.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shop.order.entity.OrderProduct;

class OrderEarnedPointCalculatorTest {

    @Test
    @DisplayName("단가 적립 × 수량으로 라인 적립을 계산한다")
    void lineEarnedPoints_multipliesUnitByQuantity() {
        OrderProduct line = OrderProduct.builder()
                .rewardPoints(500L)
                .quantity(3L)
                .build();

        assertThat(OrderEarnedPointCalculator.lineEarnedPoints(line)).isEqualTo(1500L);
    }

    @Test
    @DisplayName("수량이 null이면 1로 처리한다")
    void lineEarnedPoints_defaultsQuantityToOne() {
        OrderProduct line = OrderProduct.builder()
                .rewardPoints(200L)
                .quantity(null)
                .build();

        assertThat(OrderEarnedPointCalculator.lineEarnedPoints(line)).isEqualTo(200L);
    }

    @Test
    @DisplayName("rewardPoints가 null이면 0을 반환한다")
    void lineEarnedPoints_returnsZeroWhenRewardPointsNull() {
        OrderProduct line = OrderProduct.builder()
                .rewardPoints(null)
                .quantity(2L)
                .build();

        assertThat(OrderEarnedPointCalculator.lineEarnedPoints(line)).isEqualTo(0L);
    }

    @Test
    @DisplayName("여러 라인의 적립 합계를 계산한다")
    void totalEarnedPoints_sumsAllLines() {
        List<OrderProduct> lines = Arrays.asList(
                OrderProduct.builder().rewardPoints(500L).quantity(2L).build(),
                OrderProduct.builder().rewardPoints(100L).quantity(1L).build(),
                OrderProduct.builder().rewardPoints(null).quantity(5L).build());

        assertThat(OrderEarnedPointCalculator.totalEarnedPoints(lines)).isEqualTo(1100L);
    }

    @Test
    @DisplayName("빈 목록이면 0을 반환한다")
    void totalEarnedPoints_returnsZeroForEmptyList() {
        assertThat(OrderEarnedPointCalculator.totalEarnedPoints(Collections.emptyList())).isZero();
        assertThat(OrderEarnedPointCalculator.totalEarnedPoints(null)).isZero();
    }

    @Test
    @DisplayName("적립 PointLog changeReason을 생성한다")
    void earnChangeReason_formatsOrderId() {
        assertThat(OrderEarnedPointCalculator.earnChangeReason(42L)).isEqualTo("ORDER_EARN_POINTS:42");
    }
}
