package com.shop.order.adjustment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.repository.PointLogRepository;
import com.shop.log.point.service.PointLogService;
import com.shop.order.adjustment.OrderAdjustmentContext;
import com.shop.order.adjustment.OrderAdjustmentType;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.point.OrderEarnedPointCalculator;
import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OrderPointAdjustmentServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PointLogService pointLogService;
    @Mock
    private PointLogRepository pointLogRepository;
    @InjectMocks
    private OrderPointAdjustmentService service;

    @Test
    @DisplayName("부분 수정 시 releasedUsedPoints만 반환한다")
    void releasesUsedPointsOnPartialModify() {
        OrderInfo orderInfo = OrderInfo.builder().id(10L).userId(5L).build();
        User user = User.builder().id(5L).name("회원").point(100L).build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderInfo(orderInfo)
                .releasedUsedPoints(2000L)
                .earnedPointDelta(0L)
                .remainingLines(List.of(OrderProduct.builder().id(1L).build()))
                .build();

        service.adjust(ctx);

        verify(userRepository).addPoints(5L, 2000L);
        ArgumentCaptor<PointLogDto> captor = ArgumentCaptor.forClass(PointLogDto.class);
        verify(pointLogService).savePointLog(captor.capture());
        assertThat(captor.getValue().getChangeReason()).isEqualTo("ORDER_MODIFY_REFUND:10");
    }

    @Test
    @DisplayName("전체 취소 시 사용 포인트 반환 및 적립 회수")
    void fullCancelRefundsUsedAndDeductsEarned() {
        OrderInfo orderInfo = OrderInfo.builder().id(20L).userId(7L).build();
        User user = User.builder().id(7L).name("회원").point(1000L).build();

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(pointLogRepository.existsByChangeReason(OrderEarnedPointCalculator.earnChangeReason(20L)))
                .thenReturn(true);

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.FULL_CANCEL)
                .orderInfo(orderInfo)
                .originalUsedPointAmount(BigDecimal.valueOf(500))
                .originalEarnedPointAmount(800L)
                .build();

        service.adjust(ctx);

        verify(userRepository).addPoints(7L, 500L);
        verify(userRepository).addPoints(7L, -800L);
    }

    @Test
    @DisplayName("적립 미지급 주문 부분 수정 시 earned 차감하지 않는다")
    void skipsEarnedDeductWhenNotPaidOut() {
        OrderInfo orderInfo = OrderInfo.builder().id(30L).userId(8L).build();

        when(pointLogRepository.existsByChangeReason(OrderEarnedPointCalculator.earnChangeReason(30L)))
                .thenReturn(false);

        OrderAdjustmentContext ctx = OrderAdjustmentContext.builder()
                .type(OrderAdjustmentType.PARTIAL_MODIFY)
                .orderInfo(orderInfo)
                .releasedUsedPoints(0L)
                .earnedPointDelta(100L)
                .remainingLines(List.of(OrderProduct.builder().id(2L).build()))
                .build();

        service.adjust(ctx);

        verify(userRepository, never()).addPoints(any(), any(Long.class));
        verify(pointLogService, never()).savePointLog(any());
    }
}
