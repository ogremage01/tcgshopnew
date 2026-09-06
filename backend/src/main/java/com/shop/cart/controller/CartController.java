package com.shop.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.cart.dto.CartDto;
import com.shop.cart.dto.CartUpdateRequest;
import com.shop.cart.service.CartService;
import com.shop.common.identity.IdentityResolver;
import com.shop.common.identity.UserIdentity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final IdentityResolver identityResolver;
    private final CartService cartService;

    // 장바구니 조회(공용)
    @GetMapping("/me")
    public ResponseEntity<CartDto> getCart(HttpServletRequest request, Authentication authentication) {
        UserIdentity userIdentity = identityResolver.resolve(request, authentication);
        if (userIdentity.isGuest()) {
            String guestId = userIdentity.getGuestId();
            CartDto cart = cartService.getCartByGuestId(guestId);
            return ResponseEntity.ok(cart);
        } else {
            String userId = userIdentity.getUserPublicId();
            CartDto cart = cartService.getCartByUserId(userId);
            return ResponseEntity.ok(cart);
        }
    }

    // 장바구니 추가
    @PostMapping("/items/{searchMapId}")
    public ResponseEntity<CartDto> addCartItem(
        HttpServletRequest request, Authentication authentication, @PathVariable Long searchMapId, @RequestBody CartUpdateRequest cartAddRequest) {
        UserIdentity userIdentity = identityResolver.resolve(request, authentication);
        log.info("userIdentity: {}", userIdentity.getKey());
        if (userIdentity.isGuest()) {
            String guestId = userIdentity.getGuestId();
            log.info("guestId: {}", guestId);
            cartService.addItemToCartForGuest(guestId, searchMapId, cartAddRequest.getQuantity());
            return ResponseEntity.ok(null);
        } else {
            String userId = userIdentity.getUserPublicId();
            log.info("userId: {}", userId);
            cartService.addItemToCartForUser(userId, searchMapId, cartAddRequest.getQuantity());
            return ResponseEntity.ok(null);
        }
    }

    // 장바구니 아이템 수량 수정
    @PatchMapping("/items/{searchMapId}")
    public ResponseEntity<CartDto> updateCartItemQuantity(
        HttpServletRequest request, Authentication authentication, @PathVariable Long searchMapId, @RequestBody CartUpdateRequest cartUpdateRequest) {
        UserIdentity userIdentity = identityResolver.resolve(request, authentication);
        log.info("userIdentity: {}", userIdentity.getKey());
        if (userIdentity.isGuest()) {
            String guestId = userIdentity.getGuestId();
            log.info("guestId: {}", guestId);
            cartService.updateItemQuantityForGuest(guestId, searchMapId, cartUpdateRequest.getQuantity());
            return ResponseEntity.ok(null);
        } else {
            String userId = userIdentity.getUserPublicId();
            log.info("userId: {}", userId);
            cartService.updateItemQuantityForUser(userId, searchMapId, cartUpdateRequest.getQuantity());
            return ResponseEntity.ok(null);
        }
    }
    // 장바구니 아이템 삭제
    @DeleteMapping("/items/{searchMapId}")
    public ResponseEntity<CartDto> deleteCartItem(
        HttpServletRequest request, Authentication authentication, @PathVariable Long searchMapId) {
        UserIdentity userIdentity = identityResolver.resolve(request, authentication);
        log.info("userIdentity: {}", userIdentity.getKey());
        if (userIdentity.isGuest()) {
            String guestId = userIdentity.getGuestId();
            log.info("guestId: {}", guestId);
            cartService.removeItemFromCartForGuest(guestId, searchMapId);
            return ResponseEntity.ok(null);
        } else {
            String userId = userIdentity.getUserPublicId();
            log.info("userId: {}", userId);
            cartService.removeItemFromCartForUser(userId, searchMapId);
            return ResponseEntity.ok(null);
        }
    }
    // 장바구니 비우기
    @DeleteMapping("/items")
    public ResponseEntity<CartDto> clearCart(
        HttpServletRequest request, Authentication authentication) {
        UserIdentity userIdentity = identityResolver.resolve(request, authentication);
        log.info("userIdentity: {}", userIdentity.getKey());
        if (userIdentity.isGuest()) {
            String guestId = userIdentity.getGuestId();
            log.info("guestId: {}", guestId);
            cartService.clearCartForGuest(guestId);
            return ResponseEntity.ok(null);
        } else {
            String userId = userIdentity.getUserPublicId();
            log.info("userId: {}", userId);
            cartService.clearCartForUser(userId);
            return ResponseEntity.ok(null);
        }
    }
}