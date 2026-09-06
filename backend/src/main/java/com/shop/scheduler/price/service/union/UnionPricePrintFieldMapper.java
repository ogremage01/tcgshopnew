package com.shop.scheduler.price.service.union;

import java.util.Locale;

/**
 * 소스 가격 테이블 → {@code union_prices} print_type / printing 매핑 규칙.
 */
public final class UnionPricePrintFieldMapper {

    private UnionPricePrintFieldMapper() {
    }

    public record FabPrintFields(String printType, String printing) {
    }

    /**
     * fab_prices.foil → union print_type / printing.
     * 실데이터: {@code Normal} 또는 foil 변형명 ({@code Rainbow Foil} 등).
     */
    public static FabPrintFields fromFabFoil(String rawFoil) {
        if (rawFoil == null || rawFoil.isBlank()) {
            return new FabPrintFields("Normal", "Normal");
        }
        if (rawFoil.toLowerCase(Locale.ROOT).contains("foil")) {
            return new FabPrintFields("Foil", rawFoil);
        }
        return new FabPrintFields("Normal", "Normal");
    }
}
