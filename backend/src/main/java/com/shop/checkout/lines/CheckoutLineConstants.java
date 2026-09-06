package com.shop.checkout.lines;

/**
 * 결제 확정 시 라인 단위 실패 reason 코드 (API/프론트와의 계약).
 */
public final class CheckoutLineConstants {

    private CheckoutLineConstants() {
    }

    public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
    public static final String PRODUCT_UNAVAILABLE = "PRODUCT_UNAVAILABLE";
    public static final String PRICE_CHANGED = "PRICE_CHANGED";
    public static final String UNSUPPORTED_PRODUCT_TABLE = "UNSUPPORTED_PRODUCT_TABLE";
    public static final String POINT_AMOUNT_CHANGED = "POINT_AMOUNT_CHANGED";
}
