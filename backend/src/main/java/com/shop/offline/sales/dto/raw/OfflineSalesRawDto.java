package com.shop.offline.sales.dto.raw;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfflineSalesRawDto {

    private String id;
    private String orderState;
    private String orderNumber;
    private Instant createdAt;
    private List<LineItemRawDto> lineItems;
    private ChargePriceRawDto chargePrice;
    private List<PaymentRawDto> payments;


    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineItemRawDto {
        private Item item;
        private ItemPrice itemPrice;
        private List<AppliedDiscounts> appliedDiscounts;
        private Integer quantity;
        private String memo;

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Item {
            private String title;
            private Category category;
        }
        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AppliedDiscounts {
            private String title;
            private Integer amount;
        }
        
        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Category {
            private String title;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ItemPrice {
            private String title;
            private Integer priceUnit;
            private Integer priceValue;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChargePriceRawDto {
        private Integer listPrice;
        private Integer discountAmount;
        private Integer taxAmount;
        private Integer supplyAmount;
        private Integer taxExemptAmount;
        private Integer totalAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentRawDto {
        private String sourceType;
        private Integer amount;
        private Integer taxAmount;
        private Integer supplyAmount;
        private Integer taxExemptAmount;
    }
}
