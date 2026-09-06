package com.shop.card.metadata.support;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record MtgImageKeyRule(MtgImageCodeNormalizer normalizer, Set<String> setCodes) {

    public static MtgImageKeyRule of(MtgImageCodeNormalizer normalizer, String... setCodes) {
        Set<String> normalized = Arrays.stream(setCodes)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new MtgImageKeyRule(normalizer, normalized);
    }

    boolean matches(String set) {
        return setCodes.contains(set.toUpperCase(Locale.ROOT));
    }

    String build(String set, String code) {
        return normalizer.build(set, code);
    }
}
