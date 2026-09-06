package com.shop.order.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.order.dto.GuestOrderLookupRequest;
import com.shop.order.dto.OrderSimpleDto;
import com.shop.order.dto.UserOrderDetailDto;

public interface OrderCustomerService {

    Page<OrderSimpleDto> getOrders(Long userId, Pageable pageable);

    List<OrderSimpleDto> getLatestFiveOrders(Long userId);

    UserOrderDetailDto getOrderDetail(Long userId, Long orderId);

    UserOrderDetailDto lookupGuestOrder(GuestOrderLookupRequest request);
}
