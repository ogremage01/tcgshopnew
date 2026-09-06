package com.shop.card.metadata.support;

import java.util.Locale;

public enum MtgImageCodeNormalizer {
    DEFAULT {
        @Override
        String build(String set, String code) {
            return (set + "-" + code).toLowerCase(Locale.ROOT);
        }
    },
    PRESERVE_CODE_CASE {
        @Override
        String build(String set, String code) {
            return set.toLowerCase(Locale.ROOT) + "-" + code;
        }
    },
    DIGITS_ONLY {
        @Override
        String build(String set, String code) {
            String digits = code.replaceAll("\\D", "");
            if (digits.isEmpty()) {
                return "";
            }
            return (set + "-" + digits).toLowerCase(Locale.ROOT);
        }
    },
    STRIP_TRAILING_E {
        @Override
        String build(String set, String code) {
            String key = (set + "-" + code).toLowerCase(Locale.ROOT);
            if (key.endsWith("e")) {
                return key.substring(0, key.length() - 1);
            }
            return key;
        }
    };

    abstract String build(String set, String code);
}
