package com.shop.card.metadata.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shop.card.entity.MtgPrice;

class MtgImageKeyTest {

    @Test
    @DisplayName("기본: set-code 전체를 소문자로 만든다")
    void defaultKey_lowercasesSetAndCode() {
        assertThat(MtgImageKey.from("MH3", "123")).isEqualTo("mh3-123");
        assertThat(MtgImageKey.from("LEA", "ABC")).isEqualTo("lea-abc");
    }

    @Test
    @DisplayName("PLST: set만 소문자, code는 원문을 유지한다")
    void plst_preservesCodeCase() {
        assertThat(MtgImageKey.from("PLST", "MH2-123")).isEqualTo("plst-MH2-123");
        assertThat(MtgImageKey.from("plst", "INV-1")).isEqualTo("plst-INV-1");
    }

    @Test
    @DisplayName("H1R/H2R: 같은 DIGITS_ONLY 규칙으로 code에서 숫자만 쓴다")
    void h1rH2r_digitsOnly() {
        assertThat(MtgImageKey.from("H1R", "1e")).isEqualTo("h1r-1");
        assertThat(MtgImageKey.from("h1r", "1E")).isEqualTo("h1r-1");
        assertThat(MtgImageKey.from("H2R", "12e")).isEqualTo("h2r-12");
    }

    @Test
    @DisplayName("MH2: 만들어진 키가 e로 끝나면 끝의 e를 제거한다")
    void mh2_stripsTrailingE() {
        assertThat(MtgImageKey.from("MH2", "123e")).isEqualTo("mh2-123");
        assertThat(MtgImageKey.from("mh2", "123E")).isEqualTo("mh2-123");
        assertThat(MtgImageKey.from("MH2", "123")).isEqualTo("mh2-123");
    }

    @Test
    @DisplayName("H1R code에 숫자가 없으면 빈 키를 반환한다")
    void h1r_noDigits_emptyKey() {
        assertThat(MtgImageKey.from("H1R", "e")).isEqualTo("");
    }

    @Test
    @DisplayName("set/code가 비거나 MtgPrice가 null이면 빈 키를 반환한다")
    void blankOrNull_emptyKey() {
        assertThat(MtgImageKey.from(null, "1")).isEqualTo("");
        assertThat(MtgImageKey.from("H1R", null)).isEqualTo("");
        assertThat(MtgImageKey.from("  ", "1")).isEqualTo("");
        assertThat(MtgImageKey.from((MtgPrice) null)).isEqualTo("");
    }

    @Test
    @DisplayName("MtgPrice에서 set/code를 읽어 키를 만든다")
    void fromMtgPrice() {
        MtgPrice price = MtgPrice.builder().set("H1R").code("1e").build();
        assertThat(MtgImageKey.from(price)).isEqualTo("h1r-1");
    }
}
