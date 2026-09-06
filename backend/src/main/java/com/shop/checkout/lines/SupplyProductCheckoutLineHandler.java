package com.shop.checkout.lines;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.order.entity.OrderProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.service.RewardRuleMatchService;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupplyProductCheckoutLineHandler implements CheckoutLineHandler {

    private final SupplyRepository supplyRepository;
    private final CheckoutDraftItemImages checkoutDraftItemImages;
    private final RewardRuleMatchService rewardRuleMatchService;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.SUPPLY;
    }

    @Override
    public CheckoutConfirmFailedItem validate(CheckoutDraftItem line, ProductSearchMap map) {
        if (map.getTableName() != ProductTableEnum.SUPPLY) {
            return CheckoutLineFailures.unsupportedTable(line);
        }
        Supply s = supplyRepository.findById(line.getSourceId()).orElse(null);
        if (s == null || Boolean.TRUE.equals(s.getIsDeleted()) || !Boolean.TRUE.equals(s.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, availableStock(s));
        }

        long unitPrice = s.getPrice() == null ? 0L : s.getPrice();
        BigDecimal currentBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, availableStock(s));
        }

        long stock = s.getStock() == null ? 0L : s.getStock();
        if (stock < line.getQuantity()) {
            return CheckoutLineFailures.outOfStock(line, currentBd, stock);
        }

        ProductMatchContext context = ProductMatchContext.builder()
                .productType("Supplies")
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
        Supply s = supplyRepository.findById(line.getSourceId()).orElseThrow();
        long unitPrice = s.getPrice() == null ? 0L : s.getPrice();
        return BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public boolean tryReserveStock(CheckoutDraftItem line) {
        return supplyRepository.deductStockIfAvailable(line.getSourceId(), line.getQuantity()) == 1;
    }

    @Override
    public CheckoutConfirmFailedItem buildFailureAfterReserveMiss(CheckoutDraftItem line) {
        Supply s = supplyRepository.findById(line.getSourceId()).orElse(null);
        if (s == null || Boolean.TRUE.equals(s.getIsDeleted()) || !Boolean.TRUE.equals(s.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, availableStock(s));
        }
        long stock = s.getStock() == null ? 0L : s.getStock();
        long unitPrice = s.getPrice() == null ? 0L : s.getPrice();
        BigDecimal currentBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, stock);
        }
        return CheckoutLineFailures.outOfStock(line, currentBd, stock);
    }

    @Override
    public OrderProduct buildOrderProduct(long orderInfoId, CheckoutDraftItem line) {
        Supply s = supplyRepository.findById(line.getSourceId()).orElseThrow();
        long unitPriceLong = s.getPrice() == null ? 0L : s.getPrice();
        BigDecimal confirmedUnit = BigDecimal.valueOf(unitPriceLong).setScale(0, RoundingMode.HALF_UP);
        BigDecimal confirmedTotal = confirmedUnit.multiply(BigDecimal.valueOf(line.getQuantity()));
        return OrderProduct.builder()
                .orderInfoId(orderInfoId)
                .productId(s.getId())
                .productTable(ProductTableEnum.SUPPLY)
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

    private static long availableStock(Supply s) {
        if (s == null || s.getStock() == null) {
            return 0L;
        }
        return s.getStock();
    }
}
