package com.shop.offline.sales.dto.raw;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OfflineSalesResponse {

    private String resultType;
    private Object error;
    private List<OfflineSalesRawDto> success;

}
