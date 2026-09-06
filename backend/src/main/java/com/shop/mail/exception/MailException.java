package com.shop.mail.exception;

import org.springframework.http.HttpStatus;

public class MailException extends RuntimeException {

    private final Code code;

    public MailException(Code code) {
        super(code.messageCode);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public enum Code {
        INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "mail.error.internalServerError");

        private final HttpStatus status;
        /** i18n 메시지 코드. 프론트 common.json 등에서 번역 */
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
