package com.shop.common.excel.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExcelRequestDto {
    // 엑셀 요청 정보

    private String gameName;
    private String setName;
    private String setCode;
    private String printType;
    private String condition;
    private String language;
    private Long storageId;
    private Boolean isVisible;
    private Long maxVisibleStock;
}
