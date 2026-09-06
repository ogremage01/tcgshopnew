package com.shop.common.excel.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExcelRowErrorDto {

    // 엑셀 행 오류 정보
    private int rowNumber;
    private Map<String, Object> rowData;
    private List<FieldError> fieldErrors;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldError {
        // 엑셀 행 오류 필드 정보
        private String field;
        private String message;
    }
}
