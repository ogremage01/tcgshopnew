package com.shop.checkout.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CheckoutConfirmFailureResponseSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializes_code_and_failed_items() throws Exception {
        CheckoutConfirmFailureResponse dto = CheckoutConfirmFailureResponse.builder()
                .code("PRICE_CHANGED")
                .message("x")
                .failedItems(List.of(
                        CheckoutConfirmFailedItem.builder()
                                .searchMapId(1L)
                                .productNameKo("카드")
                                .reason("PRICE_CHANGED")
                                .snapshotUnitPrice(new java.math.BigDecimal("1000"))
                                .currentUnitPrice(new java.math.BigDecimal("1100"))
                                .requestedQuantity(2L)
                                .availableStock(10L)
                                .build()))
                .build();

        String json = mapper.writeValueAsString(dto);
        assertThat(json).contains("\"code\":\"PRICE_CHANGED\"");
        assertThat(json).contains("\"failedItems\"");

        CheckoutConfirmFailureResponse roundTrip = mapper.readValue(json, CheckoutConfirmFailureResponse.class);
        assertThat(roundTrip.getCode()).isEqualTo("PRICE_CHANGED");
        assertThat(roundTrip.getFailedItems()).hasSize(1);
    }
}
