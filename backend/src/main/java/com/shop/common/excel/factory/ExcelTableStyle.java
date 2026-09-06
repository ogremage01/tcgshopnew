package com.shop.common.excel.factory;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelTableStyle {

    public final CellStyle header;
    public final CellStyle body;
    // public final CellStyle bodyEven;

    public ExcelTableStyle(Workbook wb) {
        DataFormat format = wb.createDataFormat();

        // ===== Font =====
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        // Docker(Alpine) 등 Linux 서버에는 Malgun Gothic이 없어 POI/AWT 연산 시 실패할 수 있음
        headerFont.setFontHeightInPoints((short) 10);

        Font bodyFont = wb.createFont();
        bodyFont.setFontHeightInPoints((short) 10);

        // ===== Header Style =====
        header = wb.createCellStyle();
        header.setFont(headerFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);

        header.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorder(header);

        // ===== Body Style =====
        body = wb.createCellStyle();
        body.setFont(bodyFont);
        body.setAlignment(HorizontalAlignment.LEFT);
        body.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(body);

        // ===== Even Row Style (줄무늬) =====
    }

    private void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
