package com.shop.checkout.lines;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.order.entity.OrderProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.service.RewardRuleMatchService;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ManualProductCheckoutLineHandler implements CheckoutLineHandler {

    private final ManualProductRepository manualProductRepository;
    private final CheckoutDraftItemImages checkoutDraftItemImages;
    private final RewardRuleMatchService rewardRuleMatchService;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.MANUAL_PRODUCT;
    }

    @Override
    public CheckoutConfirmFailedItem validate(CheckoutDraftItem line, ProductSearchMap map) {
        if (map.getTableName() != ProductTableEnum.MANUAL_PRODUCT) {
            return CheckoutLineFailures.unsupportedTable(line);
        }
        ManualProduct m = manualProductRepository.findById(line.getSourceId()).orElse(null);
        if (m == null || Boolean.TRUE.equals(m.getIsDeleted()) || !Boolean.TRUE.equals(m.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, availableStock(m));
        }

        long unitPrice = m.getPrice() == null ? 0L : m.getPrice();
        BigDecimal currentBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, availableStock(m));
        }

        long stock = m.getStock() == null ? 0L : m.getStock();
        if (stock < line.getQuantity()) {
            return CheckoutLineFailures.outOfStock(line, currentBd, stock);
        }

        ProductMatchContext context = ProductMatchContext.builder()
                .productType("manual-products")
                .build();
        Long currentPointAmount = rewardRuleMatchService.match(context, unitPrice)
                .map(r -> r.getSaveAmount())
                .orElse(null);
        if (!java.util.Objects.equals(line.getSnapshotPointAmount(), currentPointAmount)) {
            return CheckoutLineFailures.pointAmountChanged(line, currentBd, stock);
        }

        return null;
    }

    @Override
    public long stockDeductionEntityId(CheckoutDraftItem line) {
        return line.getSourceId() != null ? line.getSourceId() : Long.MAX_VALUE;
    }

    @Override
    public BigDecimal resolveCurrentUnitPrice(CheckoutDraftItem line) {
        ManualProduct m = manualProductRepository.findById(line.getSourceId()).orElseThrow();
        long unitPrice = m.getPrice() == null ? 0L : m.getPrice();
        return BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public boolean tryReserveStock(CheckoutDraftItem line) {
        return manualProductRepository.deductStockIfAvailable(line.getSourceId(), line.getQuantity()) == 1;
    }

    @Override
    public CheckoutConfirmFailedItem buildFailureAfterReserveMiss(CheckoutDraftItem line) {
        ManualProduct m = manualProductRepository.findById(line.getSourceId()).orElse(null);
        if (m == null || Boolean.TRUE.equals(m.getIsDeleted()) || !Boolean.TRUE.equals(m.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, availableStock(m));
        }
        long stock = m.getStock() == null ? 0L : m.getStock();
        long unitPrice = m.getPrice() == null ? 0L : m.getPrice();
        BigDecimal currentBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, stock);
        }
        return CheckoutLineFailures.outOfStock(line, currentBd, stock);
    }

    @Override
    public OrderProduct buildOrderProduct(long orderInfoId, CheckoutDraftItem line) {
        ManualProduct m = manualProductRepository.findById(line.getSourceId()).orElseThrow();
        long unitPriceLong = m.getPrice() == null ? 0L : m.getPrice();
        BigDecimal confirmedUnit = BigDecimal.valueOf(unitPriceLong).setScale(0, RoundingMode.HALF_UP);
        BigDecimal confirmedTotal = confirmedUnit.multiply(BigDecimal.valueOf(line.getQuantity()));
        return OrderProduct.builder()
                .orderInfoId(orderInfoId)
                .productId(m.getId())
                .productTable(ProductTableEnum.MANUAL_PRODUCT)
                .quantity(line.getQuantity())
                .price(confirmedUnit)
                .totalPrice(confirmedTotal)
                .searchMapId(line.getSearchMapId())
                .productPublicId(line.getProductPublicId())
                .productNameKo(line.getProductNameKo())
                .productNameEn(line.getProductNameEn())
                .imageUrl(checkoutDraftItemImages.primaryImage(line))
                .productType(line.getProductType())
                .snapshotUnitPrice(confirmedUnit.longValue())
                .build();
    }

    private static long availableStock(ManualProduct m) {
        if (m == null || m.getStock() == null) {
            return 0L;
        }
        return m.getStock();
    }
}
