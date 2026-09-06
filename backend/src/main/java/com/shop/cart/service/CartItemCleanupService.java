package com.shop.cart.service;

public interface CartItemCleanupService {

    /**
     * 상품이 삭제·비노출되어 더 이상 해석할 수 없는 장바구니 줄을 제거한다.
     * 호출한 트랜잭션이 실패 응답으로 롤백돼도 삭제는 남아야 하므로 별도 트랜잭션에서 처리한다.
     */
    void removeUnavailableItem(Long cartItemId, Long searchMapId, String reason);
}
