package com.shop.card.metadata.support;

import java.util.List;

import com.shop.card.entity.MtgPrice;

public final class MtgImageKey {

    private static final List<MtgImageKeyRule> RULES = List.of(
            MtgImageKeyRule.of(MtgImageCodeNormalizer.DIGITS_ONLY, "H1R", "H2R"),
            MtgImageKeyRule.of(MtgImageCodeNormalizer.PRESERVE_CODE_CASE, "PLST"),
            MtgImageKeyRule.of(MtgImageCodeNormalizer.STRIP_TRAILING_E, "MH2"));

    private MtgImageKey() {
    }

    public static String from(MtgPrice mtgPrice) {
        if (mtgPrice == null) {
            return "";
        }
        return from(mtgPrice.getSet(), mtgPrice.getCode());
    }

    public static String from(String set, String code) {
        String trimmedSet = trimToEmpty(set);
        String trimmedCode = trimToEmpty(code);
        if (trimmedSet.isBlank() || trimmedCode.isBlank()) {
            return "";
        }
        for (MtgImageKeyRule rule : RULES) {
            if (rule.matches(trimmedSet)) {
                return rule.build(trimmedSet, trimmedCode);
            }
        }
        return MtgImageCodeNormalizer.DEFAULT.build(trimmedSet, trimmedCode);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
