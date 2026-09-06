package com.shop.checkout.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutConfirmFailureResponse {
    private String code;
    private String message;
    private List<CheckoutConfirmFailedItem> failedItems;
}
