package com.shop.checkout.lines;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.shop.card.entity.UnionPrice;
import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.checkout.pricing.CheckoutShowingPriceCalculator;
import com.shop.order.entity.OrderProduct;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.service.RewardRuleMatchService;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UnionPriceCheckoutLineHandler implements CheckoutLineHandler {

    private final CardProductRepository cardProductRepository;
    private final CheckoutShowingPriceCalculator checkoutShowingPriceCalculator;
    private final RewardRuleMatchService rewardRuleMatchService;
    private final CheckoutDraftItemImages checkoutDraftItemImages;

    @Override
    public ProductTableEnum table() {
        return ProductTableEnum.CARD_PRODUCT;
    }

    @Override
    public CheckoutConfirmFailedItem validate(CheckoutDraftItem line, ProductSearchMap map) {
        if (map.getTableName() != ProductTableEnum.CARD_PRODUCT) {
            return CheckoutLineFailures.unsupportedTable(line);
        }
        CardProduct cp = cardProductRepository.findById(line.getSourceId()).orElse(null);
        if (cp == null || Boolean.TRUE.equals(cp.getIsDeleted()) || !Boolean.TRUE.equals(cp.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, availableStock(cp));
        }

        long currentPrice = checkoutShowingPriceCalculator.resolveShowingPrice(cp);
        BigDecimal currentBd = BigDecimal.valueOf(currentPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, availableStock(cp));
        }

        long stock = cp.getCurrentVisibleStock() == null ? 0L : cp.getCurrentVisibleStock();
        if (stock < line.getQuantity()) {
            return CheckoutLineFailures.outOfStock(line, currentBd, stock);
        }

        UnionPrice up = cp.getUnionPrice();
        ProductMatchContext context = ProductMatchContext.builder()
                .game(up != null ? up.getGame() : null)
                .productType(cp.getProductType())
                .condition(cp.getCondition())
                .language(cp.getLanguage())
                .set(up != null ? up.getSetCode() : null)
                .rarity(up != null ? up.getRarity() : null)
                .printing(up != null ? up.getPrinting() : null)
                .cardName(up != null ? up.getCardName() : null)
                .setNumber(up != null && up.getSetNumber() != null ? String.valueOf(up.getSetNumber()) : null)
                .build();
        Long currentPointAmount = rewardRuleMatchService.match(context, currentPrice)
                .map(r -> r.getSaveAmount())
                .orElse(null);
        if (!java.util.Objects.equals(line.getSnapshotPointAmount(), currentPointAmount)) {
            return CheckoutLineFailures.pointAmountChanged(line, currentBd, stock);
        }

        return null;
    }

    @Override
    public BigDecimal resolveCurrentUnitPrice(CheckoutDraftItem line) {
        CardProduct cp = cardProductRepository.findById(line.getSourceId()).orElseThrow();
        long currentPrice = checkoutShowingPriceCalculator.resolveShowingPrice(cp);
        return BigDecimal.valueOf(currentPrice).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public long stockDeductionEntityId(CheckoutDraftItem line) {
        return line.getSourceId() != null ? line.getSourceId() : Long.MAX_VALUE;
    }

    @Override
    public boolean tryReserveStock(CheckoutDraftItem line) {
        return cardProductRepository.deductStockIfAvailable(line.getSourceId(), line.getQuantity()) == 1;
    }

    @Override
    public CheckoutConfirmFailedItem buildFailureAfterReserveMiss(CheckoutDraftItem line) {
        CardProduct cp = cardProductRepository.findById(line.getSourceId()).orElse(null);
        if (cp == null) {
            return CheckoutLineFailures.unavailable(line, 0L);
        }
        CardProduct fresh = cardProductRepository.findById(cp.getId()).orElse(cp);
        if (!Boolean.TRUE.equals(fresh.getIsVisible()) || Boolean.TRUE.equals(fresh.getIsDeleted())) {
            return CheckoutLineFailures.unavailable(line, availableStock(fresh));
        }
        long stock = fresh.getCurrentVisibleStock() == null ? 0L : fresh.getCurrentVisibleStock();
        long currentPrice = checkoutShowingPriceCalculator.resolveShowingPrice(fresh);
        BigDecimal currentBd = BigDecimal.valueOf(currentPrice).setScale(0, RoundingMode.HALF_UP);
        if (currentBd.compareTo(line.getSnapshotUnitPrice().setScale(0, RoundingMode.HALF_UP)) != 0) {
            return CheckoutLineFailures.priceChanged(line, currentBd, stock);
        }
        return CheckoutLineFailures.outOfStock(line, currentBd, stock);
    }

    @Override
    public OrderProduct buildOrderProduct(long orderInfoId, CheckoutDraftItem line) {
        CardProduct cp = cardProductRepository.findById(line.getSourceId()).orElseThrow();
        long currentPriceLong = checkoutShowingPriceCalculator.resolveShowingPrice(cp);
        BigDecimal confirmedUnit = BigDecimal.valueOf(currentPriceLong).setScale(0, RoundingMode.HALF_UP);
        BigDecimal confirmedTotal = confirmedUnit.multiply(BigDecimal.valueOf(line.getQuantity()));

        UnionPrice up = cp.getUnionPrice();
        ProductMatchContext context = ProductMatchContext.builder()
                .game(up != null ? up.getGame() : null)
                .productType(cp.getProductType())
                .condition(cp.getCondition())
                .language(cp.getLanguage())
                .set(up != null ? up.getSetCode() : null)
                .rarity(up != null ? up.getRarity() : null)
                .printing(up != null ? up.getPrinting() : null)
                .cardName(up != null ? up.getCardName() : null)
                .setNumber(up != null && up.getSetNumber() != null ? String.valueOf(up.getSetNumber()) : null)
                .build();
        Long rewardPoints = rewardRuleMatchService.match(context, currentPriceLong)
                .map(r -> r.getSaveAmount())
                .orElse(null);

        return OrderProduct.builder()
                .orderInfoId(orderInfoId)
                .productId(cp.getId())
                .productTable(ProductTableEnum.CARD_PRODUCT)
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
                .rewardPoints(rewardPoints)
                .build();
    }

    private static long availableStock(CardProduct cp) {
        if (cp == null || cp.getCurrentVisibleStock() == null) {
            return 0L;
        }
        return cp.getCurrentVisibleStock();
    }
}
