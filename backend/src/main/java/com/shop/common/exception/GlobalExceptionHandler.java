package com.shop.common.exception;

import com.shop.auth.exception.AuthException;
import com.shop.common.excel.exception.ExcelException;
import com.shop.user.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.shop.checkout.dto.CheckoutConfirmFailureResponse;
import com.shop.checkout.exception.CheckoutConfirmConflictException;
import com.shop.checkout.exception.TossPaymentsUncertainStateException;

import java.util.Map;

/**
 * 전역 예외 처리. 컨트롤러/서비스에서 던진 예외를 HTTP 응답으로 변환해 프론트에 일관된 형식으로 전달한다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** user 도메인 예외 → Code에 따른 status + messageCode (프론트에서 common.json으로 번역) */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<Map<String, String>> handleUserException(UserException e) {
        log.warn("UserException handled: code={}, message={}", e.getCode(), e.getMessage(), e);
        return ResponseEntity
                .status(e.getCode().getStatus())
                .body(Map.of("messageCode", e.getCode().getMessageCode()));
    }

    /** 인증 실패 등 → 예외에 담긴 HTTP 상태, body: { "message": "..." } */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException e) {
        log.warn("AuthException handled: code={}, messageCode={}", e.getCode(), e.getCode().getMessageCode());
        return ResponseEntity.status(e.getCode().getStatus())
                .body(Map.of("messageCode", e.getCode().getMessageCode()));
    }

    /** 엑셀 처리 예외 → Code에 따른 status + messageCode */
    @ExceptionHandler(ExcelException.class)
    public ResponseEntity<Map<String, String>> handleExcelException(ExcelException e) {
        log.warn("ExcelException handled: code={}, messageCode={}", e.getCode(), e.getCode().getMessageCode(), e);
        return ResponseEntity.status(e.getCode().getStatus())
                .body(Map.of("messageCode", e.getCode().getMessageCode()));
    }

    @ExceptionHandler(CheckoutConfirmConflictException.class)
    public ResponseEntity<CheckoutConfirmFailureResponse> handleCheckoutConflict(CheckoutConfirmConflictException e) {
        log.warn("CheckoutConfirmConflict: {}", e.getBody());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getBody());
    }

    @ExceptionHandler(TossPaymentsUncertainStateException.class)
    public ResponseEntity<Map<String, String>> handleTossUncertain(TossPaymentsUncertainStateException e) {
        log.warn("TossPaymentsUncertainStateException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "PAYMENT_STATUS_UNKNOWN"));
    }

    /** Spring ResponseStatusException — 서비스에서 상태 코드와 함께 던질 때 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        log.warn("ResponseStatusException: status={}, reason={}", e.getStatusCode(), e.getReason());
        String msg = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", msg));
    }

    /** 파일 업로드 크기 초과 → 413 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        Throwable cause = e.getCause();
        log.warn("MaxUploadSizeExceededException: maxAllowedSize={} bytes, cause={}",
                e.getMaxUploadSize(),
                cause != null ? cause.getMessage() : "unknown");
        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(Map.of("message", "업로드 파일 크기가 제한을 초과했습니다."));
    }

    /** 그 외 비즈니스/런타임 예외 → 500, body: { "message": "..." } */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        log.error("RuntimeException handled", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다."));
    }

    /**
     * 클라이언트가 응답 전에 연결을 끊은 경우. 본문을 쓰지 않아 2차 직렬화 오류를 피한다.
     */
    @ExceptionHandler({ ClientAbortException.class, AsyncRequestNotUsableException.class })
    public void handleClientDisconnected(Exception ex) {
        log.warn("Client disconnected before response completed: {}", ex.toString());
    }

    /** 모든 예외 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("Exception handled", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "서버 오류가 발생했습니다."));
    }
}
