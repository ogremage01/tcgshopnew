package com.shop.card.metadata.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shop.card.entity.UnionPrice;

class PrintingResolverTest {

        @Test
        @DisplayName("MH2 collector number에 e가 있으면 Foil Etched")
        void mh2_segmentContainsE_foilEtched() {
                assertThat(PrintingResolver.resolve(context("MH2", "MH2-123e-Foil", "Foil", "Foil")))
                                .isEqualTo("Foil Etched");
        }

        @Test
        @DisplayName("H1R collector number에 e가 있으면 Foil Etched")
        void h1r_segmentContainsE_foilEtched() {
                assertThat(PrintingResolver.resolve(context("H1R", "H1R-1e-Normal", "Normal", "Normal")))
                                .isEqualTo("Foil Etched");
        }

        @Test
        @DisplayName("setCode 대소문자를 무시한다")
        void setCode_caseInsensitive() {
                assertThat(PrintingResolver.resolve(context("mh2", "mh2-123E-Foil", "Foil", "Foil")))
                                .isEqualTo("Foil Etched");
                assertThat(PrintingResolver.resolve(context("h1r", "h1r-1E-Foil", "Foil", "Foil")))
                                .isEqualTo("Foil Etched");
        }

        @Test
        @DisplayName("같은 세트라도 collector number에 e가 없으면 원본 printing을 쓴다")
        void sameSet_noE_keepsPrinting() {
                assertThat(PrintingResolver.resolve(context("MH2", "MH2-123-Foil", "Foil", "Foil")))
                                .isEqualTo("Foil");
                assertThat(PrintingResolver.resolve(context("H1R", "H1R-1-Normal", "Normal", "Normal")))
                                .isEqualTo("Normal");
        }

        @Test
        @DisplayName("다른 세트의 collector number에 e가 있어도 덮어쓰지 않는다")
        void otherSet_segmentContainsE_keepsPrinting() {
                assertThat(PrintingResolver.resolve(context("SLD", "SLD-123e-Foil", "Foil", "Foil")))
                                .isEqualTo("Foil");
        }

        @Test
        @DisplayName("e가 다른 세그먼트에만 있으면 덮어쓰지 않는다")
        void eInOtherSegment_keepsPrinting() {
                assertThat(PrintingResolver.resolve(context("MH2", "MH2-123-FoilEtched", "Foil", "Foil")))
                                .isEqualTo("Foil");
        }

        @Test
        @DisplayName("dash가 2개 미만이면 원본 printing을 쓴다")
        void tooFewDashes_keepsPrinting() {
                assertThat(PrintingResolver.resolve(context("MH2", "MH2-123e", "Foil", "Foil")))
                                .isEqualTo("Foil");
                assertThat(PrintingResolver.resolve(context("MH2", "MH2", "Foil", "Foil")))
                                .isEqualTo("Foil");
        }

        @Test
        @DisplayName("override가 없으면 printing이 비어 있을 때 printType으로 대체한다")
        void blankPrinting_fallsBackToPrintType() {
                assertThat(PrintingResolver.resolve(context("LEA", "LEA-1-Foil", null, "Foil")))
                                .isEqualTo("Foil");
                assertThat(PrintingResolver.resolve(context("LEA", "LEA-1-Foil", "  ", "Normal")))
                                .isEqualTo("Normal");
        }

        @Test
        @DisplayName("UnionPrice에서 필드를 읽어 해석한다")
        void fromUnionPrice() {
                UnionPrice unionPrice = UnionPrice.builder()
                                .setCode("MH2")
                                .checkCodeRefined("MH2-45e-Foil")
                                .printing("Foil")
                                .printType("Foil")
                                .build();
                assertThat(PrintingResolver.resolve(unionPrice)).isEqualTo("Foil Etched");
        }

        @Test
        @DisplayName("null이면 null을 반환한다")
        void nullInput_returnsNull() {
                assertThat(PrintingResolver.resolve((UnionPrice) null)).isNull();
                assertThat(PrintingResolver.resolve((PrintingResolveContext) null)).isNull();
        }

        private static PrintingResolveContext context(
                        String setCode,
                        String checkCodeRefined,
                        String printing,
                        String printType) {
                return new PrintingResolveContext(setCode, checkCodeRefined, printing, printType);
        }
}
