package com.shop.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * user 도메인 비즈니스 예외 통합. Code에 따라 HTTP 상태와 메시지가 정해진다.
 */
public class AuthException extends RuntimeException {

    private final Code code;

    public AuthException(Code code) {
        super(code.messageCode);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public enum Code {
        EMAIL_AND_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "auth.error.emailAndPasswordRequired"),
        INVALID_EMAIL_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "auth.error.invalidEmailOrPassword"),
        INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "auth.error.invalidRefreshToken"),
        INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "auth.error.invalidAccessToken"),
        USER_NOT_FOUND(HttpStatus.NOT_FOUND, "auth.error.userNotFound"),
        DUPLICATE_EMAIL(HttpStatus.CONFLICT, "auth.error.duplicateEmail"),
        PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "auth.error.passwordPolicyViolation"),
        INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "auth.error.internalServerError");

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
