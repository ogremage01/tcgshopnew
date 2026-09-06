package com.shop.common.excel.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.shop.common.excel.dto.ExcelFileResult;
import com.shop.common.excel.dto.ExcelRequestDto;
import com.shop.common.excel.dto.ExcelUploadResultDto;
import com.shop.offline.product.dto.OfflineProductDto;
import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;
import com.shop.admin.analyze.dto.AdminOfflineSalesTotalDto;
public interface ExcelService {

    // 엑셀 파일 저장
    ExcelUploadResultDto saveProductExcelFile(MultipartFile file) throws IOException;

    // 엑셀 파일 검증
    boolean isExcelFile(MultipartFile file) throws IOException;

    // 엑셀 파일 생성
    ExcelFileResult getProductExcelFile(ExcelRequestDto excelRequestDto);

    // 오류 체크용 내부 메서드. 필요할 때 마다 바꿔서 사용하자.
    void createExcelFile(List<SetNameProductTypePairDto> data) throws IOException;

    // 오프라인 매출 총계 엑셀 파일 생성
    ExcelFileResult generateOfflineSalesTotalExcel(List<AdminOfflineSalesTotalDto> offlineSalesTotalList, String startDate, String endDate);

    // 오프라인 상품 목록 엑셀 파일 생성
    ExcelFileResult generateOfflineProductsExcel(List<OfflineProductDto> offlineProducts);

}
