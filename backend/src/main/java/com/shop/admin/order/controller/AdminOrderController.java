package com.shop.admin.order.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.admin.order.dto.AdminOrderCancelRequest;
import com.shop.admin.order.dto.AdminOrderDeliveryInfoUpdateRequest;
import com.shop.admin.order.dto.AdminOrderDeliveryMemoUpdateRequest;
import com.shop.admin.order.dto.AdminOrderDeliveryTrackingUpdateRequest;
import com.shop.admin.order.dto.AdminOrderDetailDto;
import com.shop.admin.order.dto.AdminOrderProductModifyRequest;
import com.shop.admin.order.dto.AdminOrderAdjustmentResponse;
import com.shop.admin.order.dto.AdminOrderStatusUpdateRequest;
import com.shop.admin.order.dto.AdminOrderSearchRequest;
import com.shop.admin.order.dto.AdminSimpleOrderRequest;
import com.shop.admin.order.dto.AdminTodayOrderSummaryDto;
import com.shop.admin.order.dto.AdminTotalOrderSummaryDto;
import com.shop.admin.order.service.AdminOrderService;
import com.shop.common.response.ErrorResponse;
import com.shop.order.dto.AdminOrderSimpleDto;
import com.shop.order.dto.OrderConfigDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    // TODO: 주문 목록 조회
    // TODO: 주문 상세 조회
    // TODO: 주문 접수
    // TODO: 주문 상태 변경
    // TODO: 주문 검색(주문 번호, 주문자 이름, 주문자 이메일, 주문 상태)
    // TODO: 일자별 주문 필터
    // TODO: 주문 상태 필터

    private final AdminOrderService adminOrderService;

    //===============================================
    // 주문 관리
    //===============================================
    @PostMapping("/list")
    public ResponseEntity<Page<AdminOrderSimpleDto>> getOrderList(@RequestBody AdminSimpleOrderRequest adminSimpleOrderRequest) {
        return ResponseEntity.ok(adminOrderService.getAdminOrderSimpleList(adminSimpleOrderRequest));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<AdminOrderSimpleDto>> searchOrders(@RequestBody AdminOrderSearchRequest request) {
        return ResponseEntity.ok(adminOrderService.searchAdminOrders(request));
    }

    @GetMapping("/today-summary")
    public ResponseEntity<AdminTodayOrderSummaryDto> getTodayOrderSummary() {
        return ResponseEntity.ok(adminOrderService.getTodayOrderSummary());
    }

    @GetMapping("/total-summary")
    public ResponseEntity<AdminTotalOrderSummaryDto> getTotalOrderSummary() {
        return ResponseEntity.ok(adminOrderService.getTotalOrderSummary());
    }

    //===============================================
    // 주문 상세 조회
    //===============================================
    @GetMapping("/detail/{id}")
    public ResponseEntity<AdminOrderDetailDto> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderService.getAdminOrderDetail(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody AdminOrderStatusUpdateRequest request) {
        adminOrderService.updateOrderStatus(id, request.getOrderStatus());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AdminOrderAdjustmentResponse> cancelOrder(
            @PathVariable Long id,
            @RequestBody AdminOrderCancelRequest request) {
        return ResponseEntity.ok(adminOrderService.cancelOrder(
                id,
                request.isRestoreStock(),
                request.isRestoreCart(),
                request.getCancelReason()));
    }

    @PutMapping("/{id}/delivery-tracking")
    public ResponseEntity<Void> updateDeliveryTrackingNumber(
            @PathVariable Long id,
            @RequestBody AdminOrderDeliveryTrackingUpdateRequest request) {
        adminOrderService.updateDeliveryTrackingNumber(id, request.getDeliveryTrackingNumber());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/delivery-memo")
    public ResponseEntity<Void> updateDeliveryMemo(
            @PathVariable Long id,
            @RequestBody AdminOrderDeliveryMemoUpdateRequest request) {
        adminOrderService.updateDeliveryMemo(id, request.getDeliveryMemo());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/delivery-info")
    public ResponseEntity<Void> updateDeliveryInfo(
            @PathVariable Long id,
            @RequestBody AdminOrderDeliveryInfoUpdateRequest request) {
        adminOrderService.updateDeliveryInfo(
                id, request.getDeliveryTrackingNumber(), request.getDeliveryMemo());
        return ResponseEntity.ok().build();
    }

    //===============================================
    // 주문 설정 관리
    //===============================================
    @GetMapping("/config")
    public ResponseEntity<List<OrderConfigDto>> getConfigList() {
        return ResponseEntity.ok(adminOrderService.getConfigList());
    }

    /**
     * 배송비 설정-실패를 대비해 ? 반환
     * @param orderConfigDto
     * @return
     */
    @PostMapping("/shipping-fee")
    public ResponseEntity<?> setShippingFee(@RequestBody OrderConfigDto orderConfigDto) {
        try {
            adminOrderService.setShippingFee(orderConfigDto);
            return ResponseEntity.ok(orderConfigDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder().message("배송비 설정 실패").build());
        }
    }
    @PostMapping("/free-shipping-threshold")
    public ResponseEntity<?> setFreeShippingThreshold(@RequestBody OrderConfigDto orderConfigDto) {
        try {
            adminOrderService.setFreeShippingThreshold(orderConfigDto);
            return ResponseEntity.ok(orderConfigDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.builder().message("배송 무료 설정 실패").build());
        }
    }

    //===============================================

    @PutMapping("/{id}/products")
    public ResponseEntity<AdminOrderAdjustmentResponse> modifyOrderProducts(
            @PathVariable Long id,
            @RequestBody AdminOrderProductModifyRequest request) {
        return ResponseEntity.ok(adminOrderService.modifyOrderProducts(id, request));
    }
}

