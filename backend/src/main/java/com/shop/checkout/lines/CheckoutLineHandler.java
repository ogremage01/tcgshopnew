package com.shop.checkout.lines;

import java.math.BigDecimal;

import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.order.entity.OrderProduct;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;

/**
 * {@link ProductTableEnum}별 결제 확정(검증·재고·주문 라인) 처리.
 */
public interface CheckoutLineHandler {

    ProductTableEnum table();

    /**
     * 맵·라인 정합성은 호출 전에 검증된 상태여야 한다.
     *
     * @return 실패 시 실패 항목, 성공 시 {@code null}
     */
    CheckoutConfirmFailedItem validate(CheckoutDraftItem line, ProductSearchMap map);

    /**
     * DB에서 현재 단가를 조회해 반환한다.
     * confirm 단계에서 {@link #validate}가 이미 통과된 라인에 대해 호출되어야 한다.
     */
    BigDecimal resolveCurrentUnitPrice(CheckoutDraftItem line);

    /** 재고 차감 순서 고정용 (동시성 시 데드락 완화). */
    long stockDeductionEntityId(CheckoutDraftItem line);

    /**
     * 재고 예약(원자 차감).
     *
     * @return 영향 행이 1이면 true
     */
    boolean tryReserveStock(CheckoutDraftItem line);

    /** {@link #tryReserveStock} 실패 시 원인 분류용. */
    CheckoutConfirmFailedItem buildFailureAfterReserveMiss(CheckoutDraftItem line);

    OrderProduct buildOrderProduct(long orderInfoId, CheckoutDraftItem line);
}
