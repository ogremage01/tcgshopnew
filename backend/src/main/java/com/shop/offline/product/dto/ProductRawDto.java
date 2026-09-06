package com.shop.offline.product.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRawDto {

    private String id;
    private CatalogCategory category;
    private String title;
    private CatalogItemPrice price;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogCategory {
        private String id;
        private String title;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CatalogItemPrice {
        private Integer priceUnit;
        private Integer priceValue;
        private String barcode;
    }

}

