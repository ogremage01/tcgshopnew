package com.shop.order.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.order.dto.OrderSimpleDto;
import com.shop.order.service.OrderCustomerService;
import com.shop.order.dto.UserOrderDetailDto;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderCustomerService orderCustomerService;
    private final UserService userService;

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return userService.findUserIdByPublicId(auth.getName()).orElse(null);
    }

    @GetMapping
    public ResponseEntity<Page<OrderSimpleDto>> getOrders(Pageable pageable) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(orderCustomerService.getOrders(userId, pageable));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<OrderSimpleDto>> getLatestFiveOrders() {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(orderCustomerService.getLatestFiveOrders(userId));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<UserOrderDetailDto> getOrderDetail(@PathVariable Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(orderCustomerService.getOrderDetail(userId, id));
    }
}
