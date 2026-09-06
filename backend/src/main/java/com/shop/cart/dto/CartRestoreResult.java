package com.shop.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartRestoreResult {

    private int linesRequested;
    private int linesFullyRestored;
    private int linesPartiallyRestored;
    private int linesSkipped;

    public boolean isApplied() {
        return linesFullyRestored > 0 || linesPartiallyRestored > 0;
    }
}
