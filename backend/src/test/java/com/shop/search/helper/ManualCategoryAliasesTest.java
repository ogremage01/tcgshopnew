package com.shop.search.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ManualCategoryAliasesTest {

    @Test
    @DisplayName("운영 URL Preorder Products 는 로컬 시드 preorder 도 포함한다")
    void expand_preorderProductsIncludesSeedName() {
        assertThat(ManualCategoryAliases.expand(List.of("Preorder Products")))
                .contains("Preorder Products", "preorder");
    }

    @Test
    @DisplayName("로컬 시드 preorder 는 운영명 Preorder Products 도 포함한다")
    void expand_preorderIncludesProductionName() {
        assertThat(ManualCategoryAliases.expand(List.of("preorder")))
                .contains("Preorder Products", "preorder");
    }
}
