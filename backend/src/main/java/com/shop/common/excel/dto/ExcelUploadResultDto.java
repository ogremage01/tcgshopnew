package com.shop.common.excel.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcelUploadResultDto {

    private int successCount;
    private int failCount;
    private int updatedCount;
    private boolean saved;
    private List<ExcelRowErrorDto> errors = new ArrayList<>();

}
