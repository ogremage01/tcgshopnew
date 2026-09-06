package com.shop.offline.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class OfflineShippingQuantityAdjusterTest {

    @Test
    void diffContributions_newCompletedOrder_addsQuantities() {
        Map<String, Integer> deltas = OfflineShippingQuantityAdjuster.diffContributions(
                Map.of(),
                Map.of("Product A", 2, "Product B", 1));

        assertThat(deltas).containsExactlyInAnyOrderEntriesOf(
                Map.of("Product A", 2, "Product B", 1));
    }

    @Test
    void diffContributions_cancelCompletedOrder_subtractsQuantities() {
        Map<String, Integer> deltas = OfflineShippingQuantityAdjuster.diffContributions(
                Map.of("Product A", 3),
                Map.of());

        assertThat(deltas).containsEntry("Product A", -3);
    }

    @Test
    void diffContributions_quantityChangeWhileCompleted_appliesDelta() {
        Map<String, Integer> deltas = OfflineShippingQuantityAdjuster.diffContributions(
                Map.of("Product A", 2),
                Map.of("Product A", 5));

        assertThat(deltas).containsEntry("Product A", 3);
    }

    @Test
    void diffContributions_unchanged_returnsEmpty() {
        Map<String, Integer> deltas = OfflineShippingQuantityAdjuster.diffContributions(
                Map.of("Product A", 2),
                Map.of("Product A", 2));

        assertThat(deltas).isEmpty();
    }
}
