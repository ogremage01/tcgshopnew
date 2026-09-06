package com.shop.offline.product.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineProductReceivingRequest {

    private List<OfflineProductReceivingItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OfflineProductReceivingItemRequest {
        /** offline_products.id */
        private Long productId;
        private Integer receivingQuantity;
    }
}
