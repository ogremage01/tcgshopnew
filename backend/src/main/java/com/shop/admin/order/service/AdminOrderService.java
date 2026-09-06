package com.shop.admin.order.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.shop.admin.order.dto.AdminOrderDetailDto;
import com.shop.admin.order.dto.AdminOrderProductModifyRequest;
import com.shop.admin.order.dto.AdminOrderAdjustmentResponse;
import com.shop.admin.order.dto.AdminOrderSearchRequest;
import com.shop.admin.order.dto.AdminSimpleOrderRequest;
import com.shop.admin.order.dto.AdminTodayOrderSummaryDto;
import com.shop.admin.order.dto.AdminTotalOrderSummaryDto;
import com.shop.order.dto.AdminOrderSimpleDto;
import com.shop.order.dto.OrderConfigDto;

public interface AdminOrderService {

    // 주문 설정 목록 조회
    List<OrderConfigDto> getConfigList();

    // 배송비 설정
    void setShippingFee(OrderConfigDto orderConfigDto);

    // 배송비 무료 기준 금액 설정
    void setFreeShippingThreshold(OrderConfigDto orderConfigDto);

    // 주문 목록 조회
    Page<AdminOrderSimpleDto> getAdminOrderSimpleList(AdminSimpleOrderRequest adminSimpleOrderRequest);

    /** 주문 번호(id) 또는 고객명(recipientName) 키워드 검색 */
    Page<AdminOrderSimpleDto> searchAdminOrders(AdminOrderSearchRequest request);

    /** 당일(서버 로컬 달력) 주문 현황 */
    AdminTodayOrderSummaryDto getTodayOrderSummary();

    /** 총 주문 현황 */
    AdminTotalOrderSummaryDto getTotalOrderSummary();

    // 주문 상세 조회
    AdminOrderDetailDto getAdminOrderDetail(Long id);

    // 주문 상태 변경
    void updateOrderStatus(Long id, String orderStatus);

    /** 주문 취소 (선택적 재고·장바구니 복구, 취소 사유 필수) */
    AdminOrderAdjustmentResponse cancelOrder(
            Long id, boolean restoreStock, boolean restoreCart, String cancelReason);

    // 송장번호 수정
    void updateDeliveryTrackingNumber(Long id, String deliveryTrackingNumber);

    // 배송 안내 메시지 수정
    void updateDeliveryMemo(Long id, String deliveryMemo);

    // 송장번호·안내 메시지를 한 트랜잭션에서 수정
    void updateDeliveryInfo(Long id, String deliveryTrackingNumber, String deliveryMemo);

    // 부분 주문 수정 (수량 감소·라인 삭제·재고 복구)
    AdminOrderAdjustmentResponse modifyOrderProducts(Long orderId, AdminOrderProductModifyRequest request);
}
