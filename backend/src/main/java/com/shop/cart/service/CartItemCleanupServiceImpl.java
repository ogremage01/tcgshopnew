package com.shop.cart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shop.cart.repository.CartItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartItemCleanupServiceImpl implements CartItemCleanupService {

    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeUnavailableItem(Long cartItemId, Long searchMapId, String reason) {
        if (cartItemId == null) {
            return;
        }
        cartItemRepository.findById(cartItemId).ifPresent(cartItemRepository::delete);
        log.warn("Removed unavailable cart item: cartItemId={}, searchMapId={}, reason={}",
                cartItemId, searchMapId, reason);
    }
}
