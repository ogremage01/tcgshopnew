package com.shop.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.cart.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByGuestId(String guestId);
    Optional<Cart> findByUserId(Long userId);
    

}
