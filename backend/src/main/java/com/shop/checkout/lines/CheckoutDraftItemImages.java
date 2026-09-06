package com.shop.checkout.lines;

import org.springframework.stereotype.Component;

import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.product.mapper.ProductImageUrlResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CheckoutDraftItemImages {

    private final ProductImageUrlResolver productImageUrlResolver;

    String primaryImage(CheckoutDraftItem line) {
        String image = firstNonBlank(line.getImageUrlEn(), line.getImageUrlKo(), line.getImageUrl());
        return productImageUrlResolver.ensureLocalCardImageOrDefault(image);
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        if (c != null && !c.isBlank()) {
            return c;
        }
        return null;
    }
}
