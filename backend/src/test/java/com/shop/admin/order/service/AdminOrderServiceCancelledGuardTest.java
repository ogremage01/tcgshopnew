package com.shop.admin.order.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.order.dto.AdminOrderProductModifyRequest;
import com.shop.card.metadata.repository.MtgSetInfoRepository;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.log.point.repository.PointLogRepository;
import com.shop.log.point.service.PointLogService;
import com.shop.order.adjustment.OrderAdjustmentOrchestrator;
import com.shop.order.adjustment.guard.OrderCancelledGuard;
import com.shop.order.entity.OrderInfo;
import com.shop.order.enums.OrderStatus;
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
import com.shop.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceCancelledGuardTest {

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

    private OrderInfo cancelledOrder() {
        return OrderInfo.builder()
                .id(1L)
                .orderStatus(OrderStatus.ORDER_CANCELLED.getValue())
                .build();
    }

    private void stubCancelledGuard() {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "ORDER_ALREADY_CANCELLED"))
                .when(orderCancelledGuard)
                .assertNotCancelled(org.mockito.ArgumentMatchers.any(OrderInfo.class));
    }

    @Nested
    @DisplayName("취소된 주문 변경 차단")
    class BlockCancelledOrder {

        @Test
        @DisplayName("updateOrderStatus — 409")
        void blocksStatusUpdate() {
            when(orderInfoRepository.findById(1L)).thenReturn(Optional.of(cancelledOrder()));
            stubCancelledGuard();

            assertThatThrownBy(() -> adminOrderService.updateOrderStatus(1L, OrderStatus.ORDER_COMPLETED.getValue()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("ORDER_ALREADY_CANCELLED");
        }

        @Test
        @DisplayName("updateDeliveryTrackingNumber — 409")
        void blocksDeliveryTrackingUpdate() {
            when(orderInfoRepository.findById(1L)).thenReturn(Optional.of(cancelledOrder()));
            stubCancelledGuard();

            assertThatThrownBy(() -> adminOrderService.updateDeliveryTrackingNumber(1L, "123"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("ORDER_ALREADY_CANCELLED");
        }

        @Test
        @DisplayName("updateDeliveryMemo — 409")
        void blocksDeliveryMemoUpdate() {
            when(orderInfoRepository.findById(1L)).thenReturn(Optional.of(cancelledOrder()));
            stubCancelledGuard();

            assertThatThrownBy(() -> adminOrderService.updateDeliveryMemo(1L, "memo"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("ORDER_ALREADY_CANCELLED");
        }

        @Test
        @DisplayName("modifyOrderProducts — orchestrator가 ValidateStep에서 차단")
        void delegatesModifyToOrchestrator() {
            AdminOrderProductModifyRequest request = AdminOrderProductModifyRequest.builder().build();
            adminOrderService.modifyOrderProducts(1L, request);
            verify(orderAdjustmentOrchestrator).executePartialModify(org.mockito.ArgumentMatchers.any());
        }
    }
}
