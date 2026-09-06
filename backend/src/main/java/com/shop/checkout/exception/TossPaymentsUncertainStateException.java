package com.shop.checkout.exception;

/**
 * 토스 API 결과를 성공/실패로 확정할 수 없을 때(타임아웃, 연결 끊김 등).
 * 이 예외를 결제 실패로 취급하거나 즉시 취소하면 안 된다.
 */
public class TossPaymentsUncertainStateException extends RuntimeException {

    public TossPaymentsUncertainStateException(String action, String logKey, Throwable cause) {
        super("Toss " + action + " result uncertain for " + logKey, cause);
    }
}
