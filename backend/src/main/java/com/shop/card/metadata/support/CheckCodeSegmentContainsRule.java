package com.shop.card.metadata.support;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record CheckCodeSegmentContainsRule(
                Set<String> setCodes,
                int segmentIndex,
                String needle,
                String overridePrinting) implements PrintingOverrideRule {

        public static CheckCodeSegmentContainsRule of(
                        int segmentIndex,
                        String needle,
                        String overridePrinting,
                        String... setCodes) {
                Set<String> normalized = Arrays.stream(setCodes)
                                .map(code -> code.toUpperCase(Locale.ROOT))
                                .collect(Collectors.toUnmodifiableSet());
                String normalizedNeedle = needle == null ? "" : needle.toLowerCase(Locale.ROOT);
                return new CheckCodeSegmentContainsRule(normalized, segmentIndex, normalizedNeedle, overridePrinting);
        }

        @Override
        public Optional<String> apply(PrintingResolveContext context) {
                if (context == null || context.setCode() == null || context.setCode().isBlank()) {
                        return Optional.empty();
                }
                if (!setCodes.contains(context.setCode().toUpperCase(Locale.ROOT))) {
                        return Optional.empty();
                }
                String segment = segmentAt(context.checkCodeRefined(), segmentIndex);
                if (!needle.isEmpty() && segment.toLowerCase(Locale.ROOT).contains(needle)) {
                        return Optional.of(overridePrinting);
                }
                return Optional.empty();
        }

        static String segmentAt(String checkCodeRefined, int index) {
                if (checkCodeRefined == null || checkCodeRefined.isBlank() || index < 0) {
                        return "";
                }
                String[] parts = checkCodeRefined.split("-", -1);
                // index N is the span between dash N and dash N+1; need a following dash.
                if (parts.length < index + 2) {
                        return "";
                }
                return parts[index];
        }
}
