package com.shop.scheduler.image.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScryfallImageDownloadServiceImplTest {

    @Test
    @DisplayName("선행 t/T를 제거한다")
    void stripLeadingT_removesPrefix() {
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT("tneo")).isEqualTo("neo");
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT("TNEO")).isEqualTo("NEO");
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT(" tlea ")).isEqualTo("lea");
    }

    @Test
    @DisplayName("t로 시작하지 않거나 제거 후 빈 값이면 null")
    void stripLeadingT_returnsNullWhenNotApplicable() {
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT(null)).isNull();
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT("neo")).isNull();
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT("t")).isNull();
        assertThat(ScryfallImageDownloadServiceImpl.stripLeadingT("")).isNull();
    }
}
