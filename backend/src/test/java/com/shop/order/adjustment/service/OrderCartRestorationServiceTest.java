package com.shop.order.adjustment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.cart.dto.CartRestoreLine;
import com.shop.cart.dto.CartRestoreResult;
import com.shop.cart.service.CartService;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.repository.OrderProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderCartRestorationServiceTest {

    @Mock
    private CartService cartService;

    @Mock
    private OrderProductRepository orderProductRepository;

    @InjectMocks
    private OrderCartRestorationService service;

    @Test
    @DisplayName("비회원 주문은 장바구니 복구를 수행하지 않는다")
    void skipsGuestOrder() {
        OrderInfo orderInfo = OrderInfo.builder()
                .id(1L)
                .guest(true)
                .userId(null)
                .build();

        CartRestoreResult result = service.restoreAllForOrder(orderInfo, 1L);

        assertThat(result.isApplied()).isFalse();
        verify(cartService, never()).restoreItemsForUserId(any(), any());
    }

    @Test
    @DisplayName("회원 주문은 주문 라인을 장바구니에 복구한다")
    void restoresMemberOrderLines() {
        OrderInfo orderInfo = OrderInfo.builder()
                .id(2L)
                .guest(false)
                .userId(99L)
                .build();
        List<OrderProduct> lines = List.of(
                OrderProduct.builder().id(10L).searchMapId(100L).quantity(2L).build(),
                OrderProduct.builder().id(11L).searchMapId(200L).quantity(1L).build());

        when(orderProductRepository.findByOrderInfoId(2L)).thenReturn(lines);
        when(cartService.restoreItemsForUserId(eq(99L), any())).thenReturn(CartRestoreResult.builder()
                .linesRequested(2)
                .linesFullyRestored(2)
                .build());

        CartRestoreResult result = service.restoreAllForOrder(orderInfo, 2L);

        assertThat(result.isApplied()).isTrue();
        assertThat(result.getLinesFullyRestored()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartRestoreLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(cartService).restoreItemsForUserId(eq(99L), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getSearchMapId()).isEqualTo(100L);
        assertThat(captor.getValue().get(0).getQuantity()).isEqualTo(2L);
    }

    @Test
    @DisplayName("searchMapId가 없는 라인은 스킵한다")
    void skipsLinesWithoutSearchMapId() {
        OrderInfo orderInfo = OrderInfo.builder()
                .id(3L)
                .guest(false)
                .userId(50L)
                .build();
        List<OrderProduct> lines = List.of(
                OrderProduct.builder().id(20L).searchMapId(null).quantity(3L).build(),
                OrderProduct.builder().id(21L).searchMapId(300L).quantity(1L).build());

        when(orderProductRepository.findByOrderInfoId(3L)).thenReturn(lines);
        when(cartService.restoreItemsForUserId(eq(50L), any())).thenReturn(CartRestoreResult.builder()
                .linesRequested(1)
                .linesFullyRestored(1)
                .build());

        CartRestoreResult result = service.restoreAllForOrder(orderInfo, 3L);

        assertThat(result.getLinesFullyRestored()).isEqualTo(1);
        assertThat(result.getLinesSkipped()).isEqualTo(1);
    }
}
