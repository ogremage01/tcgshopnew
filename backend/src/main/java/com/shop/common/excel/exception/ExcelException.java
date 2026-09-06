package com.shop.common.excel.exception;

import org.springframework.http.HttpStatus;

public class ExcelException extends RuntimeException {

    private final Code code;

    public ExcelException(Code code) {
        super(code.messageCode);
        this.code = code;
    }

    public ExcelException(Code code, Throwable cause) {
        super(code.messageCode, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public enum Code {
        // 다운로드 실패시
        EXCEL_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "excel.error.downloadFailed"),
        // 데이터 없을 시
        EXCEL_EXPORT_DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "excel.error.exportDataNotFound");

        private final HttpStatus status;
        private final String messageCode;

        Code(HttpStatus status, String messageCode) {
            this.status = status;
            this.messageCode = messageCode;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getMessageCode() {
            return messageCode;
        }
    }
}
