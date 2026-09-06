package com.shop.order.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.order.service.AdminOrderService;
import com.shop.order.dto.GuestOrderLookupRequest;
import com.shop.order.dto.OrderSimpleDto;
import com.shop.order.dto.UserOrderDetailDto;
import com.shop.order.entity.OrderInfo;
import com.shop.order.mapper.OrderDetailMapper;
import com.shop.order.repository.OrderInfoRepository;

import com.shop.admin.order.dto.AdminOrderDetailDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCustomerServiceImpl implements OrderCustomerService {

    private static final String ORDER_DETAIL_LOG = "[ORDER-DETAIL]";
    private static final String GUEST_ORDER_NOT_FOUND = "GUEST_ORDER_NOT_FOUND";

    private final OrderInfoRepository orderInfoRepository;
    private final AdminOrderService adminOrderService;

    //사용자 주문 목록 조회
    @Override
    public Page<OrderSimpleDto> getOrders(Long userId, Pageable pageable) {
        Page<OrderInfo> orders = orderInfoRepository.findByUserIdOrderByPaymentDateDesc(userId, pageable);
        return orders.map(this::toOrderSimpleDto);
    }

    //사용자 최근 5건 주문 목록 조회
    @Override
    public List<OrderSimpleDto> getLatestFiveOrders(Long userId) {
        Page<OrderInfo> orders = orderInfoRepository.findByUserIdOrderByPaymentDateDesc(userId, PageRequest.of(0, 5));
        return orders.getContent().stream().map(this::toOrderSimpleDto).collect(Collectors.toList());
    }

    //사용자 주문 상세 조회
    @Override
    public UserOrderDetailDto getOrderDetail(Long userId, Long orderId) {
        OrderInfo orderInfo = orderInfoRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND"));
        if (orderInfo.getUserId() == null || !orderInfo.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED");
        }
        AdminOrderDetailDto adminDetail = adminOrderService.getAdminOrderDetail(orderId);
        log.info("{} getOrderDetail userId={} orderId={} calling OrderDetailMapper",
                ORDER_DETAIL_LOG, userId, orderId);
        return OrderDetailMapper.fromAdmin(adminDetail);
    }

    //게스트 주문 조회
    @Override
    public UserOrderDetailDto lookupGuestOrder(GuestOrderLookupRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
        }
        String code = request.getVerificationCode();
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
        }

        OrderInfo order = orderInfoRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GUEST_ORDER_NOT_FOUND));

        if (!Boolean.TRUE.equals(order.getGuest())
                || order.getGuestVerificationCode() == null
                || !order.getGuestVerificationCode().equals(code.trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, GUEST_ORDER_NOT_FOUND);
        }

        AdminOrderDetailDto adminDetail = adminOrderService.getAdminOrderDetail(order.getId());
        log.info("{} lookupGuestOrder orderId={} calling OrderDetailMapper",
                ORDER_DETAIL_LOG, order.getId());
        return OrderDetailMapper.fromAdmin(adminDetail);
    }

    //주문 간략 정보 변환 로직
    private OrderSimpleDto toOrderSimpleDto(OrderInfo orderInfo) {
        return OrderSimpleDto.builder()
                .id(orderInfo.getId())
                .orderDate(orderInfo.getPaymentDate())
                .orderStatus(orderInfo.getOrderStatus())
                .orderTotal(orderInfo.getTotalPaymentAmount())
                .paymentCurrency(orderInfo.getPaymentCurrency())
                .build();
    }
}
