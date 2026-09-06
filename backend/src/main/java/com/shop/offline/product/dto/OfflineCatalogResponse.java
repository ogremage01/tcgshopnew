package com.shop.offline.product.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfflineCatalogResponse {

    private String resultType;
    private Object error;
    private List<ProductRawDto> success;

}
