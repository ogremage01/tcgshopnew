package com.shop.common.excel.service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.shop.card.entity.UnionPrice;
import com.shop.card.service.UnionPriceQueryService;
import com.shop.common.excel.dto.ExcelFileResult;
import com.shop.common.excel.dto.ExcelRequestDto;
import com.shop.common.excel.dto.ExcelRowErrorDto;
import com.shop.common.excel.dto.ExcelUploadResultDto;
import com.shop.common.excel.exception.ExcelException;
import com.shop.common.excel.factory.ExcelTableStyle;
import com.shop.admin.analyze.dto.AdminOfflineSalesTotalDto;
import com.shop.offline.product.dto.OfflineProductDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.service.CardProductService;
import com.shop.scheduler.metadata.dto.SetNameProductTypePairDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
@Service
public class ExcelServiceImpl implements ExcelService {

    /** 숫자/불리언/문자열 셀을 모두 안전하게 문자열로 읽기 (getStringCellValue는 NUMERIC 셀에서 예외 발생) */
    private static final DataFormatter EXCEL_DATA_FORMATTER = new DataFormatter();

    private static String cellToString(Cell cell) {
        if (cell == null) {
            return null;
        }
        String v = EXCEL_DATA_FORMATTER.formatCellValue(cell);
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim();
    }

    private static Optional<Long> tryParseLong(String value, long defaultValue) {
        if (value == null || value.isBlank()) {
            return Optional.of(defaultValue);
        }
        try {
            return Optional.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Double> tryParseDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return Optional.of(defaultValue);
        }
        try {
            return Optional.of(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static void addInvalidNumberFieldError(
            String fieldName,
            String rawValue,
            List<ExcelRowErrorDto.FieldError> fieldErrors,
            Map<String, Object> rowData) {
        fieldErrors.add(new ExcelRowErrorDto.FieldError(fieldName, "숫자 형식이 올바르지 않습니다."));
        rowData.put(fieldName, rawValue);
    }

    /** Alpine/헤드리스 서버에서는 autoSizeColumn이 폰트 부재로 실패할 수 있어 고정 폭 사용 */
    private static final int[] EXPORT_COLUMN_WIDTHS = {
            4000, 3500, 3000, 2500, 3000, 6000, 8000, 3000, 3000, 3000,
            3500, 3000, 3000, 3000, 3000, 3500, 5000, 3500
    };

    private static String sanitizeSheetName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "export";
        }
        String sanitized = raw.replaceAll("[\\\\/?*\\[\\]:]", "_").trim();
        if (sanitized.isBlank()) {
            return "export";
        }
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    private static void setStringCell(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private static void setLongCell(Row row, int column, Long value) {
        if (value == null) {
            row.createCell(column).setBlank();
            return;
        }
        row.createCell(column).setCellValue(value.doubleValue());
    }

    private static void setIntegerCell(Row row, int column, Integer value, CellStyle style) {
        if (value == null) {
            row.createCell(column).setBlank();
            return;
        }
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void applyExportColumnWidths(Sheet sheet) {
        for (int i = 0; i < EXPORT_COLUMN_WIDTHS.length; i++) {
            sheet.setColumnWidth(i, EXPORT_COLUMN_WIDTHS[i]);
        }
    }

    /**
     * 셀 문자열 길이 기준으로 열 너비를 잡는다.
     * (Alpine/헤드리스에서 autoSizeColumn 폰트 의존을 피하기 위함)
     */
    private static void applyContentBasedColumnWidths(Sheet sheet, int columnCount) {
        int[] maxDisplayWidths = new int[columnCount];
        for (Row row : sheet) {
            for (int col = 0; col < columnCount; col++) {
                Cell cell = row.getCell(col);
                if (cell == null) {
                    continue;
                }
                maxDisplayWidths[col] = Math.max(maxDisplayWidths[col], estimateDisplayWidth(cell));
            }
        }
        for (int col = 0; col < columnCount; col++) {
            // POI 단위: 문자 폭의 1/256. 여백 2자 + 최소/최대 클램프
            int width = Math.min(Math.max((maxDisplayWidths[col] + 2) * 256, 2500), 255 * 256);
            sheet.setColumnWidth(col, width);
        }
    }

    private static int estimateDisplayWidth(Cell cell) {
        String value = EXCEL_DATA_FORMATTER.formatCellValue(cell);
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            // 한글·전각·CJK는 대략 2칸으로 취급
            width += (cp > 0x7F) ? 2 : 1;
            i += Character.charCount(cp);
        }
        return width;
    }

    @Value("${file.path.upload}")
    private String uploadDirectory;

    private final UnionPriceQueryService unionPriceQueryService;
    private final CardProductService cardProductService;

    @Override
    public ExcelUploadResultDto saveProductExcelFile(MultipartFile file) throws IOException {
        // 0. 파일 검증
        if (!isExcelFile(file)) {
            throw new IllegalArgumentException("파일 형식이 올바르지 않습니다.");
        }
        // 저장 대상
        List<CardProduct> cardProductList = new ArrayList<>();
        // 결과 반환
        ExcelUploadResultDto result = new ExcelUploadResultDto();
        // 1. 파일 읽기(엑셀 파일)및 타입 검사
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            // 1. 헤더 행 제외 — 컬럼 순서는 getProductExcelFile() 생성 템플릿과 동일
            int lastRowNum = sheet.getLastRowNum();
            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                // 5: 정제체크코드 → Foil 여부
                String checkCodeRefined = cellToString(row.getCell(5));
                String printType = (checkCodeRefined != null && checkCodeRefined.contains("Foil")) ? "Foil" : "Normal";
                // 3: 언어, 4: 컨디션, 6: 카드명(표시용·등록 시 미사용)
                String language = cellToString(row.getCell(3));
                String condition = cellToString(row.getCell(4));
                String currentVisibleStockString = cellToString(row.getCell(7));
                String maxVisibleStockString = cellToString(row.getCell(8));
                String totalStockString = cellToString(row.getCell(9));
                String unionPriceIdString = cellToString(row.getCell(10));
                String priceLinkedString = cellToString(row.getCell(11));
                Boolean priceLinked = priceLinkedString == null ? false : Boolean.valueOf(priceLinkedString);
                String priceRateString = cellToString(row.getCell(12));
                String priceString = cellToString(row.getCell(13));
                String isVisibleString = cellToString(row.getCell(14));
                Boolean isVisible = isVisibleString == null ? false : Boolean.valueOf(isVisibleString);
                String storageIdString = cellToString(row.getCell(15));
                String memoString = cellToString(row.getCell(16));
                String memo = memoString == null ? "" : memoString;
                String productType = cellToString(row.getCell(17));

                List<ExcelRowErrorDto.FieldError> numberFormatErrors = new ArrayList<>();
                HashMap<String, Object> numberFormatRowData = new HashMap<>();
                Optional<Long> currentVisibleStockOpt = tryParseLong(currentVisibleStockString, 0L);
                if (currentVisibleStockOpt.isEmpty()) {
                    addInvalidNumberFieldError("currentVisibleStock", currentVisibleStockString, numberFormatErrors,
                            numberFormatRowData);
                }
                Optional<Long> maxVisibleStockOpt = tryParseLong(maxVisibleStockString, 0L);
                if (maxVisibleStockOpt.isEmpty()) {
                    addInvalidNumberFieldError("maxVisibleStock", maxVisibleStockString, numberFormatErrors,
                            numberFormatRowData);
                }
                Optional<Long> totalStockOpt = tryParseLong(totalStockString, 0L);
                if (totalStockOpt.isEmpty()) {
                    addInvalidNumberFieldError("totalStock", totalStockString, numberFormatErrors,
                            numberFormatRowData);
                }
                Optional<Long> unionPriceIdOpt = tryParseLong(unionPriceIdString, 0L);
                if (unionPriceIdOpt.isEmpty()) {
                    addInvalidNumberFieldError("unionPriceId", unionPriceIdString, numberFormatErrors,
                            numberFormatRowData);
                }
                Optional<Double> priceRateOpt = tryParseDouble(priceRateString, 1.0);
                if (priceRateOpt.isEmpty()) {
                    addInvalidNumberFieldError("priceRate", priceRateString, numberFormatErrors, numberFormatRowData);
                }
                Optional<Long> priceOpt = tryParseLong(priceString, 0L);
                if (priceOpt.isEmpty()) {
                    addInvalidNumberFieldError("price", priceString, numberFormatErrors, numberFormatRowData);
                }
                Optional<Long> storageIdOpt = tryParseLong(storageIdString, 0L);
                if (storageIdOpt.isEmpty()) {
                    addInvalidNumberFieldError("storageId", storageIdString, numberFormatErrors, numberFormatRowData);
                }
                if (!numberFormatErrors.isEmpty()) {
                    result.getErrors()
                            .add(new ExcelRowErrorDto(i, numberFormatRowData, numberFormatErrors));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }

                Long currentVisibleStock = currentVisibleStockOpt.get();
                Long maxVisibleStock = maxVisibleStockOpt.get();
                Long totalStock = totalStockOpt.get();
                Long unionPriceId = unionPriceIdOpt.get();
                Double priceRate = priceRateOpt.get();
                Long price = priceOpt.get();
                Long storageId = storageIdOpt.get();
                boolean hasNegativeNumber = currentVisibleStock < 0
                        || maxVisibleStock < 0
                        || totalStock < 0
                        || unionPriceId < 0
                        || priceRate < 0
                        || price < 0
                        || storageId < 0;
                UnionPrice unionPrice = (unionPriceIdString == null || hasNegativeNumber) ? null
                        : unionPriceQueryService.findById(unionPriceId);

                CardProduct cardProduct = new CardProduct();
                cardProduct.setCondition(condition);
                cardProduct.setPrintType(printType);
                cardProduct.setLanguage(language);
                cardProduct.setIsVisible(isVisible);
                cardProduct.setCurrentVisibleStock(currentVisibleStock);
                cardProduct.setMaxVisibleStock(maxVisibleStock);
                cardProduct.setTotalStock(totalStock);
                cardProduct.setUnionPrice(unionPrice);
                cardProduct.setIsPriceLinked(priceLinked);
                cardProduct.setPricingRate(priceRate);
                cardProduct.setPrice(price);
                cardProduct.setMemo(memo);
                cardProduct.setStorageId(storageId);
                cardProduct.setIsAutoUpdatedStock(true);
                cardProduct.setIsDeleted(false);
                cardProduct.setProductType(productType);
                if (unionPrice == null) {
                    ExcelRowErrorDto.FieldError fieldError = new ExcelRowErrorDto.FieldError("unionPriceId",
                            "UnionPrice not found");
                    HashMap<String, Object> rowData = new HashMap<>();
                    rowData.put("unionPriceId", unionPriceIdString);
                    result.getErrors()
                            .add(new ExcelRowErrorDto(i, rowData, new ArrayList<>(Arrays.asList(fieldError))));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }
                if (!Objects.equals(checkCodeRefined, unionPrice.getCheckCodeRefined())) {
                    ExcelRowErrorDto.FieldError fieldError = new ExcelRowErrorDto.FieldError("checkCodeRefined",
                            "정제체크코드가 일치하지 않습니다.");
                    HashMap<String, Object> rowData = new HashMap<>();
                    rowData.put("excelCheckCodeRefined", checkCodeRefined);
                    rowData.put("dbCheckCodeRefined", unionPrice.getCheckCodeRefined());
                    result.getErrors()
                            .add(new ExcelRowErrorDto(i, rowData, new ArrayList<>(Arrays.asList(fieldError))));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }
                if (hasNegativeNumber) {
                    ExcelRowErrorDto.FieldError fieldError = new ExcelRowErrorDto.FieldError("negativeNumber",
                            "숫자 관련 컬럼에 음수 값이 포함되어 있습니다.");
                    HashMap<String, Object> rowData = new HashMap<>();
                    rowData.put("negativeNumber", hasNegativeNumber);
                    result.getErrors()
                            .add(new ExcelRowErrorDto(i, rowData, new ArrayList<>(Arrays.asList(fieldError))));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }
                if (currentVisibleStock > maxVisibleStock) {
                    ExcelRowErrorDto.FieldError fieldError = new ExcelRowErrorDto.FieldError("currentVisibleStock",
                            "표시재고가 최대표시 수량보다 큽니다.");
                    HashMap<String, Object> rowData = new HashMap<>();
                    rowData.put("currentVisibleStock", currentVisibleStock);
                    rowData.put("maxVisibleStock", maxVisibleStock);
                    result.getErrors()
                            .add(new ExcelRowErrorDto(i, rowData, new ArrayList<>(Arrays.asList(fieldError))));
                    result.setFailCount(result.getFailCount() + 1);
                    continue;
                }

                // 디버그 로그: CardProduct 정보 출력
                log.info(
                        "엑셀 행 {} 읽음 - unionPriceId: {}, isVisible: {}, storageId: {}, isPriceLinked: {}, pricingRate: {}, condition: {}, printType: {}, language: {}",
                        i, unionPriceId, isVisible, storageId, priceLinked, priceRate, condition, printType, language);

                cardProductList.add(cardProduct);
                result.setSuccessCount(result.getSuccessCount() + 1);
            }
        }
        if (result.getFailCount() > 0) {
            return result;
        } else {
            int updatedCount = cardProductService.registerOrUpdateCardProducts(cardProductList);
            result.setUpdatedCount(updatedCount);
            result.setSaved(true);
            return result;
        }
    }

    // 엑셀파일 생성
    @Override
    public ExcelFileResult getProductExcelFile(ExcelRequestDto excelRequestDto) {

        try {
            String normalizedPrintType = excelRequestDto.getPrintType() == null
                    ? ""
                    : excelRequestDto.getPrintType().trim();
            // setCode만 있어도 union_prices.set_code로 매칭. 역호환: setCode 없을 때만 setName 사용.
            String setCodeOrNameForQuery = "";
            if (StringUtils.hasText(excelRequestDto.getSetCode())) {
                setCodeOrNameForQuery = excelRequestDto.getSetCode().trim();
            } else if (StringUtils.hasText(excelRequestDto.getSetName())) {
                setCodeOrNameForQuery = excelRequestDto.getSetName().trim();
            }
            List<UnionPrice> unionPriceList = unionPriceQueryService
                    .findByGameAndSetNameAndPrintType(excelRequestDto.getGameName(), setCodeOrNameForQuery,
                            normalizedPrintType);

            if (unionPriceList.isEmpty()) {
                log.warn(
                        "Excel export data not found. gameName={}, setCodeOrNameForQuery={}, setName={}, setCode={}, printType={}",
                        excelRequestDto.getGameName(), setCodeOrNameForQuery, excelRequestDto.getSetName(),
                        excelRequestDto.getSetCode(), normalizedPrintType);
                throw new ExcelException(ExcelException.Code.EXCEL_EXPORT_DATA_NOT_FOUND);
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                String sheetName = sanitizeSheetName(
                        (excelRequestDto.getSetCode() == null ? "" : excelRequestDto.getSetCode()) + "_"
                                + (excelRequestDto.getPrintType() == null ? "" : excelRequestDto.getPrintType()) + "_"
                                + (excelRequestDto.getLanguage() == null ? "" : excelRequestDto.getLanguage()));
                Sheet sheet = workbook.createSheet(sheetName);
                ExcelTableStyle excelTableStyle = new ExcelTableStyle(workbook);

                int rowIndex = 1;
                // 헤더 행 생성
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("게임");
                headerRow.createCell(1).setCellValue("세트코드");
                headerRow.createCell(2).setCellValue("세트번호");
                headerRow.createCell(3).setCellValue("언어");
                headerRow.createCell(4).setCellValue("컨디션");
                headerRow.createCell(5).setCellValue("정제체크코드");
                headerRow.createCell(6).setCellValue("카드명");
                headerRow.createCell(7).setCellValue("표시재고");
                headerRow.createCell(8).setCellValue("최대표시");
                headerRow.createCell(9).setCellValue("총재고");
                headerRow.createCell(10).setCellValue("dbId");
                headerRow.createCell(11).setCellValue("가격연동");
                headerRow.createCell(12).setCellValue("연동배율");
                headerRow.createCell(13).setCellValue("고정가");
                headerRow.createCell(14).setCellValue("공개여부");
                headerRow.createCell(15).setCellValue("보관소ID");
                headerRow.createCell(16).setCellValue("메모");
                headerRow.createCell(17).setCellValue("상품타입");

                for (int i = 0; i < 18; i++) {
                    headerRow.getCell(i).setCellStyle(excelTableStyle.header);
                }
                // 데이터 행 생성
                for (int i = 0; i < unionPriceList.size(); i++) {
                    Row row = sheet.createRow(rowIndex++);
                    boolean isEven = rowIndex % 2 == 0;
                    // var cellStyle = isEven ? excelTableStyle.bodyEven : excelTableStyle.body;
                    // gameName
                    setStringCell(row, 0, excelRequestDto.getGameName());
                    log.info("gameName: {}", excelRequestDto.getGameName());
                    // setCode
                    setStringCell(row, 1, excelRequestDto.getSetCode());
                    log.info("setCode: {}", excelRequestDto.getSetCode());
                    // setNumber
                    setLongCell(row, 2, unionPriceList.get(i).getSetNumber());
                    log.info("setNumber: {}", unionPriceList.get(i).getSetNumber());
                    // language
                    setStringCell(row, 3, excelRequestDto.getLanguage());
                    log.info("language: {}", excelRequestDto.getLanguage());
                    // condition
                    setStringCell(row, 4, excelRequestDto.getCondition());
                    log.info("condition: {}", excelRequestDto.getCondition());
                    // checkCodeRefined
                    setStringCell(row, 5, unionPriceList.get(i).getCheckCodeRefined());
                    log.info("checkCodeRefined: {}", unionPriceList.get(i).getCheckCodeRefined());
                    // cardName
                    boolean useKoreanName = "ko".equalsIgnoreCase(excelRequestDto.getLanguage());
                    setStringCell(row, 6, useKoreanName
                            ? unionPriceList.get(i).getCardNameK()
                            : unionPriceList.get(i).getCardName());
                    log.info("cardName: {}", useKoreanName
                            ? unionPriceList.get(i).getCardNameK()
                            : unionPriceList.get(i).getCardName());
                    // currentVisibleStock
                    row.createCell(7).setCellValue(0);
                    log.info("currentVisibleStock: {}", 0);
                    // maxVisibleStock
                    setLongCell(row, 8, excelRequestDto.getMaxVisibleStock());
                    log.info("maxVisibleStock: {}", excelRequestDto.getMaxVisibleStock());
                    // totalStock
                    row.createCell(9).setCellValue(0);
                    log.info("totalStock: {}", 0);
                    // unionPriceId
                    row.createCell(10).setCellValue(unionPriceList.get(i).getId());
                    log.info("unionPriceId: {}", unionPriceList.get(i).getId());
                    // priceLinked
                    row.createCell(11).setCellValue(true);
                    log.info("priceLinked: {}", true);
                    // priceRate
                    row.createCell(12).setCellValue(1.0);
                    log.info("priceRate: {}", 1.0);
                    // price
                    row.createCell(13).setCellValue(0);
                    log.info("price: {}", 0);
                    // isVisible
                    row.createCell(14).setCellValue(true);
                    log.info("isVisible: {}", true);
                    // storageId
                    setLongCell(row, 15, excelRequestDto.getStorageId());
                    log.info("storageId: {}", excelRequestDto.getStorageId());
                    // memo
                    row.createCell(16).setCellValue("");
                    log.info("memo: {}", "");
                    // productType
                    setStringCell(row, 17, unionPriceList.get(i).getProductType());
                    log.info("productType: {}", unionPriceList.get(i).getProductType());
                    // for (int col = 0; col < 18; col++) {
                    // row.getCell(col).setCellStyle(cellStyle);
                    // }
                }

                applyExportColumnWidths(sheet);
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    workbook.write(outputStream);
                    String fileName = excelRequestDto.getGameName() + "_" + excelRequestDto.getSetCode() + "_"
                            + excelRequestDto.getPrintType() + "_"
                            + excelRequestDto.getLanguage() + ".xlsx";
                    return new ExcelFileResult(outputStream.toByteArray(), fileName);
                }
            }
        } catch (ExcelException e) {
            throw e;
        } catch (IOException e) {
            throw new ExcelException(ExcelException.Code.EXCEL_DOWNLOAD_FAILED, e);
        }
    }

    // 엑셀파일 검증
    @Override
    public boolean isExcelFile(MultipartFile file) throws IOException {
        // 0. 파일 검증
        String mimeType = file.getContentType();
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    // 엑셀파일 생성-테스트용
    @Override
    public void createExcelFile(List<SetNameProductTypePairDto> data) throws IOException {
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filePath = uploadDirectory + fileName + ".xlsx";
        try (Workbook workbook = new XSSFWorkbook()) {
            FileOutputStream fos = new FileOutputStream(filePath);

            Sheet sheet = workbook.createSheet(fileName);

            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("productLineId");
            row.createCell(1).setCellValue("setNameId");
            row.createCell(2).setCellValue("productTypeId");

            for (int i = 0; i < data.size(); i++) {
                row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(data.get(i).getProductLineId());
                row.createCell(1).setCellValue(data.get(i).getSetNameId());
                row.createCell(2).setCellValue(data.get(i).getProductTypeId());
            }

            workbook.write(fos);
        }
    }
    @Override
    public ExcelFileResult generateOfflineSalesTotalExcel(List<AdminOfflineSalesTotalDto> offlineSalesTotalList, String startDate, String endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            String sheetName = "offline_sales_total_" + startDate + "_" + endDate;
            Sheet sheet = workbook.createSheet(sheetName);

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(dataFormat.getFormat("₩#,##0"));

            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("카테고리");
            row.createCell(1).setCellValue("항목");
            row.createCell(2).setCellValue("수량");
            row.createCell(3).setCellValue("매출");
            for (int i = 0; i < offlineSalesTotalList.size(); i++) {
                AdminOfflineSalesTotalDto item = offlineSalesTotalList.get(i);
                row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(item.getCategory() == null ? "" : item.getCategory());
                row.createCell(1).setCellValue(item.getItem() == null ? "" : item.getItem());

                Cell quantityCell = row.createCell(2);
                if (item.getQuantity() != null) {
                    quantityCell.setCellValue(item.getQuantity().doubleValue());
                    quantityCell.setCellStyle(numberStyle);
                }

                Cell amountCell = row.createCell(3);
                if (item.getAmount() != null) {
                    amountCell.setCellValue(item.getAmount().doubleValue());
                    amountCell.setCellStyle(currencyStyle);
                }
            }
            applyContentBasedColumnWidths(sheet, 4);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return new ExcelFileResult(outputStream.toByteArray(), sheetName + ".xlsx");
            }
        } catch (IOException e) {
            throw new ExcelException(ExcelException.Code.EXCEL_DOWNLOAD_FAILED, e);
        }
    }

    @Override
    public ExcelFileResult generateOfflineProductsExcel(List<OfflineProductDto> offlineProducts) {
        try (Workbook workbook = new XSSFWorkbook()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String sheetName = sanitizeSheetName("offline_products");
            Sheet sheet = workbook.createSheet(sheetName);

            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(dataFormat.getFormat("₩#,##0"));

            String[] headers = {
                    "ID", "상품 ID", "카테고리", "품목", "가격",
                    "입고", "출고", "재고", "바코드", "생성일",
                    "연결 테이블", "연결 ID"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int i = 0; i < offlineProducts.size(); i++) {
                OfflineProductDto item = offlineProducts.get(i);
                Row row = sheet.createRow(i + 1);
                setLongCell(row, 0, item.getId());
                setStringCell(row, 1, item.getProductId());
                setStringCell(row, 2, item.getCategoryTitle());
                setStringCell(row, 3, item.getTitle());
                setIntegerCell(row, 4, item.getPriceValue(), currencyStyle);
                setIntegerCell(row, 5, item.getReceivingQuantity(), numberStyle);
                setIntegerCell(row, 6, item.getShippingQuantity(), numberStyle);
                setIntegerCell(row, 7, item.getStockQuantity(), numberStyle);
                setStringCell(row, 8, item.getBarcode());
                setStringCell(row, 9, item.getCreatedAt() == null ? "" : item.getCreatedAt().toString());
                setStringCell(row, 10, item.getLinkTableName());
                setLongCell(row, 11, item.getLinkId());
            }
            applyContentBasedColumnWidths(sheet, headers.length);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return new ExcelFileResult(outputStream.toByteArray(), "offline_products_" + timestamp + ".xlsx");
            }
        } catch (IOException e) {
            throw new ExcelException(ExcelException.Code.EXCEL_DOWNLOAD_FAILED, e);
        }
    }
}