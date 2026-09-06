package com.shop.search.helper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 운영 카테고리명({@code Preorder Products})과 로컬 시드명({@code preorder})을 같은 필터로 본다.
 */
public final class ManualCategoryAliases {

    private static final List<Set<String>> GROUPS = List.of(
            Set.of("Preorder Products", "preorder", "Pre-order", "Pre-Order"),
            Set.of("Event Ticket", "event-ticket", "eventticket"));

    private ManualCategoryAliases() {
    }

    public static List<String> expand(List<String> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            expanded.add(trimmed);
            for (Set<String> group : GROUPS) {
                if (containsIgnoreCase(group, trimmed)) {
                    expanded.addAll(group);
                    break;
                }
            }
        }
        return new ArrayList<>(expanded);
    }

    private static boolean containsIgnoreCase(Set<String> group, String value) {
        String needle = value.toLowerCase(Locale.ROOT);
        for (String alias : group) {
            if (alias.toLowerCase(Locale.ROOT).equals(needle)) {
                return true;
            }
        }
        return false;
    }
}
