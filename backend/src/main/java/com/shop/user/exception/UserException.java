package com.shop.user.exception;

import org.springframework.http.HttpStatus;

/**
 * user 도메인 비즈니스 예외 통합. Code에 따라 HTTP 상태와 메시지가 정해진다.
 */
public class UserException extends RuntimeException {

    // 회원 예외

    private final Code code;

    /**
     * 회원 예외 생성
     * 
     * @param code 예외 코드
     */
    public UserException(Code code) {
        super(code.messageCode);
        this.code = code;
    }

    /**
     * 예외 코드 반환
     * 
     * @return 예외 코드
     */
    public Code getCode() {
        return code;
    }

    /**
     * 회원 예외 코드
     */
    public enum Code {
        DUPLICATE_EMAIL(HttpStatus.CONFLICT, "user.error.duplicateEmail"),
        INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "user.error.invalidPassword"),
        PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "user.error.passwordNotMatch"),
        USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user.error.userNotFound"),
        USER_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "user.error.userAddressNotFound"),
        INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "user.error.internalServerError"),
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "user.error.unauthorized");

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
