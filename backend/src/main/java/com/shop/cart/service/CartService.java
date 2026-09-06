package com.shop.cart.service;

import java.util.List;

import com.shop.cart.dto.CartDto;
import com.shop.cart.dto.CartRestoreLine;
import com.shop.cart.dto.CartRestoreResult;

public interface CartService {
    
    CartDto getCartByGuestId(String guestId);
    CartDto getCartByUserId(String userId);
    void addItemToCartForGuest(String guestId, Long searchMapId, Long quantity);
    void addItemToCartForUser(String userId, Long searchMapId, Long quantity);
    void removeItemFromCartForGuest(String guestId, Long searchMapId);
    void removeItemFromCartForUser(String userId, Long searchMapId);
    void updateItemQuantityForGuest(String guestId, Long searchMapId, Long quantity);
    void updateItemQuantityForUser(String userId, Long searchMapId, Long quantity);
    void clearCartForGuest(String guestId);
    void clearCartForUser(String userId);

    /** 로그인 시 게스트 장바구니 폐기. Cart 엔티티 삭제, 카트 없으면 no-op. */
    void discardGuestCart(String guestId);

    /** 로그인 시 병합용. 현재 미사용, 추후 재활성화 가능. */
    void mergeGuestCartIntoUserCart(String guestId, String userPublicId);

    /** 주문 취소 복구: 내부 userId 기준, 재고 한도까지 담고 초과분은 cap한다. */
    CartRestoreResult restoreItemsForUserId(Long userId, List<CartRestoreLine> lines);

}
