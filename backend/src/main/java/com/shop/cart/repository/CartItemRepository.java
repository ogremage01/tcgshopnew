package com.shop.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.cart.entity.Cart;
import com.shop.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart(Cart cart);

    void deleteByCart(Cart cart);

    Optional<CartItem> findByCartAndSearchMapId(Cart cart, Long searchMapId);

}
