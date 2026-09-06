package com.shop.card.metadata.support;

import java.util.Locale;

public final class FabSetCode {

    public static final String SOURCE_PREFIX = "fabtcg";

    private FabSetCode() {
    }

    public static String toDisplayCode(String sourceOrDisplayCode) {
        String code = trimToEmpty(sourceOrDisplayCode);
        if (code.toLowerCase(Locale.ROOT).startsWith(SOURCE_PREFIX)) {
            code = code.substring(SOURCE_PREFIX.length());
        }
        return normalizeDisplayEdgeCase(code);
    }

    public static String toSourceCode(String displayOrSourceCode) {
        String code = trimToEmpty(displayOrSourceCode);
        if (code.isBlank() || code.toLowerCase(Locale.ROOT).startsWith(SOURCE_PREFIX)) {
            return code;
        }
        return SOURCE_PREFIX + code;
    }

    private static String normalizeDisplayEdgeCase(String code) {
        return switch (code) {
            case "RVD/DVR", "DVR" -> "RVD";
            default -> code;
        };
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
