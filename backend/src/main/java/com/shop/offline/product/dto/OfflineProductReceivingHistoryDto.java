package com.shop.offline.product.dto;

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
public class OfflineProductReceivingHistoryDto {

    private Long id;
    private LocalDateTime createdAt;
    private String receivingManager;
    private Integer itemCount;
    private List<OfflineProductReceivingItemDto> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OfflineProductReceivingItemDto {
        private Long id;
        /** offline_products.id */
        private Long productId;
        private String tossProductId;
        private String title;
        private Integer receivingQuantity;
    }
}
