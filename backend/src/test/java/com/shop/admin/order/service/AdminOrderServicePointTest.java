package com.shop.admin.order.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.card.metadata.repository.MtgSetInfoRepository;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.repository.PointLogRepository;
import com.shop.log.point.service.PointLogService;
import com.shop.order.adjustment.OrderAdjustmentCommand;
import com.shop.order.adjustment.OrderAdjustmentOrchestrator;
import com.shop.order.adjustment.guard.OrderCancelledGuard;
import com.shop.order.entity.OrderInfo;
import com.shop.order.enums.OrderStatus;
import com.shop.order.point.OrderEarnedPointCalculator;
import com.shop.order.repository.OrderConfigRepository;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;
import com.shop.order.support.OrderInfoPersonalDataCodec;
import com.shop.product.metadata.repository.StorageRepository;
import com.shop.product.metadata.service.TcgPSetNameService;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.search.repository.map.ProductSearchMapRepository;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;
import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminOrderServicePointTest {

    @Mock
    private CardProductRepository cardProductRepository;
    @Mock
    private ManualProductRepository manualProductRepository;
    @Mock
    private OrderConfigRepository orderConfigRepository;
    @Mock
    private OrderInfoRepository orderInfoRepository;
    @Mock
    private OrderProductRepository orderProductRepository;
    @Mock
    private ProductSearchMapRepository productSearchMapRepository;
    @Mock
    private MtgSetInfoRepository mtgSetInfoRepository;
    @Mock
    private UnionPriceRepository unionPriceRepository;
    @Mock
    private StorageRepository storageRepository;
    @Mock
    private TcgPSetNameService tcgPSetNameService;
    @Mock
    private ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PointLogService pointLogService;
    @Mock
    private PointLogRepository pointLogRepository;
    @Mock
    private OrderInfoPersonalDataCodec orderInfoPersonalDataCodec;
    @Mock
    private OrderAdjustmentOrchestrator orderAdjustmentOrchestrator;
    @Mock
    private OrderCancelledGuard orderCancelledGuard;

    @InjectMocks
    private AdminOrderServiceImpl adminOrderService;

    @Nested
    @DisplayName("updateOrderStatus — 발송 완료 적립")
    class EarnOnDeliveryCompleted {

        @Test
        @DisplayName("발송 완료 시 회원에게 적립 포인트를 지급한다")
        void earnsPointsWhenDeliveryCompleted() {
            OrderInfo order = OrderInfo.builder()
                    .id(10L)
                    .userId(5L)
                    .orderStatus(OrderStatus.ORDER_COMPLETED.getValue())
                    .earnedPointAmount(1500L)
                    .build();
            User user = User.builder().id(5L).name("회원").point(100L).build();

            when(orderInfoRepository.findById(10L)).thenReturn(Optional.of(order));
            when(pointLogRepository.existsByChangeReason(OrderEarnedPointCalculator.earnChangeReason(10L)))
                    .thenReturn(false);
            when(userRepository.findById(5L)).thenReturn(Optional.of(user));

            adminOrderService.updateOrderStatus(10L, OrderStatus.ORDER_DELIVERY_COMPLETED.getValue());

            verify(userRepository).addPoints(5L, 1500L);
            ArgumentCaptor<PointLogDto> captor = ArgumentCaptor.forClass(PointLogDto.class);
            verify(pointLogService).savePointLog(captor.capture());
            assertThat(captor.getValue().getChangeReason()).isEqualTo("ORDER_EARN_POINTS:10");
            assertThat(captor.getValue().getChangedPoint()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("이미 적립된 주문은 중복 지급하지 않는다")
        void skipsDuplicateEarn() {
            OrderInfo order = OrderInfo.builder()
                    .id(10L)
                    .userId(5L)
                    .orderStatus(OrderStatus.ORDER_COMPLETED.getValue())
                    .earnedPointAmount(1500L)
                    .build();

            when(orderInfoRepository.findById(10L)).thenReturn(Optional.of(order));
            when(pointLogRepository.existsByChangeReason(OrderEarnedPointCalculator.earnChangeReason(10L)))
                    .thenReturn(true);

            adminOrderService.updateOrderStatus(10L, OrderStatus.ORDER_DELIVERY_COMPLETED.getValue());

            verify(userRepository, never()).addPoints(any(), any(Long.class));
            verify(pointLogService, never()).savePointLog(any());
        }

        @Test
        @DisplayName("발송 완료가 아닌 상태 변경은 적립하지 않는다")
        void doesNotEarnOnOtherStatus() {
            OrderInfo order = OrderInfo.builder()
                    .id(10L)
                    .userId(5L)
                    .orderStatus(OrderStatus.ORDER_PENDING.getValue())
                    .earnedPointAmount(1500L)
                    .build();

            when(orderInfoRepository.findById(10L)).thenReturn(Optional.of(order));

            adminOrderService.updateOrderStatus(10L, OrderStatus.ORDER_COMPLETED.getValue());

            verify(userRepository, never()).addPoints(any(), any(Long.class));
            verify(pointLogService, never()).savePointLog(any());
            verify(pointLogRepository, never()).existsByChangeReason(any());
        }
    }

    @Nested
    @DisplayName("cancelOrder — orchestrator 위임")
    class CancelOrderDelegation {

        @Test
        @DisplayName("취소 시 adjustment orchestrator를 호출한다")
        void delegatesToOrchestrator() {
            adminOrderService.cancelOrder(20L, true, true, "고객 요청 취소");

            ArgumentCaptor<OrderAdjustmentCommand> captor = ArgumentCaptor.forClass(OrderAdjustmentCommand.class);
            verify(orderAdjustmentOrchestrator).executeFullCancel(captor.capture());
            assertThat(captor.getValue().isRestoreStockOnCancel()).isTrue();
            assertThat(captor.getValue().isRestoreCartOnCancel()).isTrue();
            assertThat(captor.getValue().getCancelReason()).isEqualTo("고객 요청 취소");
        }
    }
}
