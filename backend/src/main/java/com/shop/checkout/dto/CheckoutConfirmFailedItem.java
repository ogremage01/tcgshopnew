package com.shop.checkout.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutConfirmFailedItem {
    private Long searchMapId;
    private String productNameKo;
    private String reason;
    private BigDecimal snapshotUnitPrice;
    private BigDecimal currentUnitPrice;
    private Long requestedQuantity;
    private Long availableStock;
}
