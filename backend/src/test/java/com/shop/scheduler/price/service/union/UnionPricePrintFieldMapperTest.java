package com.shop.scheduler.price.service.union;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnionPricePrintFieldMapperTest {

    @Test
    @DisplayName("fab foil=Normal → printType/printing 모두 Normal")
    void fromFabFoil_normal() {
        var fields = UnionPricePrintFieldMapper.fromFabFoil("Normal");

        assertThat(fields.printType()).isEqualTo("Normal");
        assertThat(fields.printing()).isEqualTo("Normal");
    }

    @Test
    @DisplayName("fab foil=Rainbow Foil → printType=Foil, printing=원문")
    void fromFabFoil_foilVariant() {
        var fields = UnionPricePrintFieldMapper.fromFabFoil("Rainbow Foil");

        assertThat(fields.printType()).isEqualTo("Foil");
        assertThat(fields.printing()).isEqualTo("Rainbow Foil");
    }

    @Test
    @DisplayName("fab foil null/blank → Normal/Normal")
    void fromFabFoil_blank() {
        assertThat(UnionPricePrintFieldMapper.fromFabFoil(null).printType()).isEqualTo("Normal");
        assertThat(UnionPricePrintFieldMapper.fromFabFoil("").printing()).isEqualTo("Normal");
    }
}
