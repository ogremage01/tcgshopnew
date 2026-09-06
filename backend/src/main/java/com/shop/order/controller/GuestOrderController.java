package com.shop.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.order.dto.GuestOrderLookupRequest;
import com.shop.order.service.OrderCustomerService;
import com.shop.order.dto.UserOrderDetailDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guest/orders")
@RequiredArgsConstructor
public class GuestOrderController {

    private final OrderCustomerService orderCustomerService;

    @PostMapping("/lookup")
    public ResponseEntity<UserOrderDetailDto> lookup(@RequestBody GuestOrderLookupRequest request) {
        return ResponseEntity.ok(orderCustomerService.lookupGuestOrder(request));
    }
}
