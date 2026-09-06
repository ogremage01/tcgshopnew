package com.shop.checkout.pricing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.shop.card.entity.UnionPrice;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.service.ProductCalculatingPriceService;

import lombok.RequiredArgsConstructor;

/**
 * 장바구니/상품 목록과 동일한 단가 산출 ({@code ProductItemDtoMapper} 와 동일 규칙).
 */
@Component
@RequiredArgsConstructor
public class CheckoutShowingPriceCalculator {

    private final ProductCalculatingPriceService productCalculatingPriceService;

    public long resolveShowingPrice(CardProduct p) {
        UnionPrice u = p.getUnionPrice();
        if (Boolean.TRUE.equals(p.getIsPriceLinked())) {
            if (p.getCalculatedLinkedPrice() != null) {
                return p.getCalculatedLinkedPrice();
            }
            if (u != null && u.getPrice() != null) {
                return productCalculatingPriceService.calculateProductPrice(p, u.getPrice());
            }
        }
        if (p.getPrice() != null) {
            return p.getPrice();
        }
        return 0L;
    }

    public BigDecimal toBigDecimal(long price) {
        return BigDecimal.valueOf(price);
    }
}
