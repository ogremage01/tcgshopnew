package com.shop.cart.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {

    private Long id;
    private Long userId;
    private String guestId;
    private List<CartItemDto> cartItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
