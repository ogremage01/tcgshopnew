package com.shop.checkout.lines;

import java.math.BigDecimal;

import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.entity.CheckoutDraftItem;

public final class CheckoutLineFailures {

    private CheckoutLineFailures() {
    }

    public static CheckoutConfirmFailedItem unavailable(CheckoutDraftItem line, long availableStock) {
        return CheckoutConfirmFailedItem.builder()
                .searchMapId(line.getSearchMapId())
                .productNameKo(line.getProductNameKo())
                .reason(CheckoutLineConstants.PRODUCT_UNAVAILABLE)
                .snapshotUnitPrice(line.getSnapshotUnitPrice())
                .currentUnitPrice(null)
                .requestedQuantity(line.getQuantity())
                .availableStock(availableStock)
                .build();
    }

    public static CheckoutConfirmFailedItem unsupportedTable(CheckoutDraftItem line) {
        return CheckoutConfirmFailedItem.builder()
                .searchMapId(line.getSearchMapId())
                .productNameKo(line.getProductNameKo())
                .reason(CheckoutLineConstants.UNSUPPORTED_PRODUCT_TABLE)
                .snapshotUnitPrice(line.getSnapshotUnitPrice())
                .currentUnitPrice(null)
                .requestedQuantity(line.getQuantity())
                .availableStock(0L)
                .build();
    }

    public static CheckoutConfirmFailedItem priceChanged(
            CheckoutDraftItem line,
            BigDecimal currentUnitPrice,
            Long availableStock) {
        return CheckoutConfirmFailedItem.builder()
                .searchMapId(line.getSearchMapId())
                .productNameKo(line.getProductNameKo())
                .reason(CheckoutLineConstants.PRICE_CHANGED)
                .snapshotUnitPrice(line.getSnapshotUnitPrice())
                .currentUnitPrice(currentUnitPrice)
                .requestedQuantity(line.getQuantity())
                .availableStock(availableStock)
                .build();
    }

    public static CheckoutConfirmFailedItem outOfStock(
            CheckoutDraftItem line,
            BigDecimal currentUnitPrice,
            long availableStock) {
        return CheckoutConfirmFailedItem.builder()
                .searchMapId(line.getSearchMapId())
                .productNameKo(line.getProductNameKo())
                .reason(CheckoutLineConstants.OUT_OF_STOCK)
                .snapshotUnitPrice(line.getSnapshotUnitPrice())
                .currentUnitPrice(currentUnitPrice)
                .requestedQuantity(line.getQuantity())
                .availableStock(availableStock)
                .build();
    }

    public static CheckoutConfirmFailedItem pointAmountChanged(
            CheckoutDraftItem line,
            BigDecimal currentUnitPrice,
            long availableStock) {
        return CheckoutConfirmFailedItem.builder()
                .searchMapId(line.getSearchMapId())
                .productNameKo(line.getProductNameKo())
                .reason(CheckoutLineConstants.POINT_AMOUNT_CHANGED)
                .snapshotUnitPrice(line.getSnapshotUnitPrice())
                .currentUnitPrice(currentUnitPrice)
                .requestedQuantity(line.getQuantity())
                .availableStock(availableStock)
                .build();
    }
}
