package com.shop.checkout.lines;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.order.entity.OrderProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.service.RewardRuleMatchService;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SealedProductCheckoutLineHandler implements CheckoutLineHandler {

    private final SealedProductRepository sealedProductRepository;
    private final CheckoutDraftItemImages checkoutDraftItemImages;
    private final RewardRuleMatchService rewardRuleMatchService;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.SEALED_PRODUCT;
    }

    @Override
    public CheckoutConfirmFailedItem validate(CheckoutDraftItem line, ProductSearchMap map) {
        if (map.getTableName() != ProductTableEnum.SEALED_PRODUCT) {
            return CheckoutLineFailures.unsupportedTable(line);
        }
        SealedProduct sealed = sealedProductRepository.findById(line.getSourceId()).orElse(null);
        if (sealed == null || Boolean.TRUE.equals(sealed.getIsDeleted()) || !Boolean.TRUE.equals(sealed.getIsActive())) {
            return CheckoutLineFailures.unavailable(line, availableStock(sealed));
        }

        long unitPrice = sealed.getPrice() == null ? 0L : sealed.getPrice();
        BigDecimal currentBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, availableStock(sealed));
        }

        long stock = availableStock(sealed);
        if (stock < line.getQuantity()) {
            return CheckoutLineFailures.outOfStock(line, currentBd, stock);
        }

        ProductMatchContext context = ProductMatchContext.builder()
                .game(sealed.getGame())
                .productType("sealed-products")
                .set(sealed.getSetCode())
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
        SealedProduct sealed = sealedProductRepository.findById(line.getSourceId()).orElseThrow();
        long unitPrice = sealed.getPrice() == null ? 0L : sealed.getPrice();
        return BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public boolean tryReserveStock(CheckoutDraftItem line) {
        return sealedProductRepository.deductStockIfAvailable(line.getSourceId(), line.getQuantity()) == 1;
    }

    @Override
    public CheckoutConfirmFailedItem buildFailureAfterReserveMiss(CheckoutDraftItem line) {
        SealedProduct sealed = sealedProductRepository.findById(line.getSourceId()).orElse(null);
        if (sealed == null || Boolean.TRUE.equals(sealed.getIsDeleted()) || !Boolean.TRUE.equals(sealed.getIsActive())) {
            return CheckoutLineFailures.unavailable(line, availableStock(sealed));
        }
        long stock = availableStock(sealed);
        long unitPrice = sealed.getPrice() == null ? 0L : sealed.getPrice();
        BigDecimal currentBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, stock);
        }
        return CheckoutLineFailures.outOfStock(line, currentBd, stock);
    }

    @Override
    public OrderProduct buildOrderProduct(long orderInfoId, CheckoutDraftItem line) {
        SealedProduct sealed = sealedProductRepository.findById(line.getSourceId()).orElseThrow();
        long unitPriceLong = sealed.getPrice() == null ? 0L : sealed.getPrice();
        BigDecimal confirmedUnit = BigDecimal.valueOf(unitPriceLong).setScale(0, RoundingMode.HALF_UP);
        BigDecimal confirmedTotal = confirmedUnit.multiply(BigDecimal.valueOf(line.getQuantity()));
        return OrderProduct.builder()
                .orderInfoId(orderInfoId)
                .productId(sealed.getId())
                .productTable(ProductTableEnum.SEALED_PRODUCT)
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

    private static long availableStock(SealedProduct sealed) {
        return sealed == null ? 0L : sealed.visibleStock();
    }
}
