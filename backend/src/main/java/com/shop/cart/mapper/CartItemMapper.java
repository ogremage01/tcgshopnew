package com.shop.cart.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shop.card.service.UnionPriceQueryService;
import com.shop.cart.dto.CartItemDto;
import com.shop.cart.entity.CartItem;
import com.shop.product.dto.ProductItemDto;
import com.shop.product.dto.SealedProductSaleDto;
import com.shop.product.dto.card.CardProductSaleDto;
import com.shop.product.service.ProductCalculatingPriceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartItemMapper {

    private final ProductCalculatingPriceService productCalculatingPriceService;
    private final UnionPriceQueryService unionPriceQueryService;

    private static final String PRODUCT_TYPE_CARDS = "Cards";
    private static final String PRODUCT_TYPE_SEALED_PRODUCTS = "SealedProducts";
    private static final String LANGUAGE_KO = "ko";

    public CartItemDto from(CartItem cartItem, ProductItemDto productItem) {
        if (cartItem == null || productItem == null) {
            return null;
        }

        Long linePrice = productItem.getPrice();
        return CartItemDto.builder()
                .id(cartItem.getId())
                .searchMapId(cartItem.getSearchMapId())
                .productType(productItem.getProductType())
                .currentVisibleStock(productItem.getCurrentVisibleStock())
                .price(linePrice)
                .priceUsd(resolveLinePriceUsd(cartItem, productItem, linePrice))
                .quantity(cartItem.getQuantity())
                .cartItemUpdatedAt(cartItem.getUpdatedAt())
                .imageUrl(resolveImageUrl(cartItem, productItem))
                .productNameEn(productItem.getProductNameEn())
                .productNameKo(productItem.getProductNameKo())
                .cardProduct(toCardProduct(cartItem, productItem))
                .suppliesProduct(toSuppliesProduct(productItem))
                .sealedProduct(toSealedProduct(cartItem, productItem))
                .build();
    }

    /** 장바구니 썸네일은 앞면 URL만 사용(Flip/더블페이스 뒷면은 사용하지 않음). */
    private String resolveImageUrl(CartItem cartItem, ProductItemDto productItem) {
        String lineLang = resolveLineSaleLanguage(productItem, cartItem);
        if (lineLang != null) {
            Map<String, String> languageImageUrlMap = productItem.getLanguageImageUrlMap();
            if (languageImageUrlMap != null) {
                return firstNonBlank(languageImageUrlMap.get(lineLang), languageImageUrlMap.get("en"));
            }
            if (LANGUAGE_KO.equals(lineLang)) {
                return firstNonBlank(productItem.getImageUrlKo(), productItem.getImageUrlEn());
            }
            return productItem.getImageUrlEn();
        }
        return firstNonBlank(productItem.getImageUrlEn(), productItem.getImageUrlKo());
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    /**
     * 장바구니 라인(searchMapId)에 해당하는 판매 레코드 언어(소문자, en/ko).
     */
    private String resolveLineSaleLanguage(ProductItemDto productItem, CartItem cartItem) {
        Long searchMapId = cartItem != null ? cartItem.getSearchMapId() : null;
        String productType = productItem.getProductType();
        if (searchMapId == null || productType == null) {
            return null;
        }
        if (PRODUCT_TYPE_CARDS.equals(productType)) {
            return languageBucketMatchingSearchMap(productItem.getCard() != null
                    ? productItem.getCard().getCardProductSaleDtoMap()
                    : null,
                    searchMapId);
        }
        if (PRODUCT_TYPE_SEALED_PRODUCTS.equals(productType)) {
            return sealedLanguageMatchingSearchMap(productItem.getSealedProductInfoDto() != null
                    ? productItem.getSealedProductInfoDto().getSealedProductSaleDtoMap()
                    : null,
                    searchMapId);
        }
        return null;
    }

    private static String languageBucketMatchingSearchMap(Map<String, List<CardProductSaleDto>> saleMap, Long searchMapId) {
        if (saleMap == null || searchMapId == null) {
            return null;
        }
        for (Map.Entry<String, List<CardProductSaleDto>> e : saleMap.entrySet()) {
            List<CardProductSaleDto> list = e.getValue();
            if (list == null) {
                continue;
            }
            for (CardProductSaleDto sale : list) {
                if (sale != null && searchMapId.equals(sale.getSearchMapId())) {
                    return normalizeLangKey(e.getKey());
                }
            }
        }
        return null;
    }

    private static String sealedLanguageMatchingSearchMap(Map<String, List<SealedProductSaleDto>> saleMap, Long searchMapId) {
        if (saleMap == null || searchMapId == null) {
            return null;
        }
        for (Map.Entry<String, List<SealedProductSaleDto>> e : saleMap.entrySet()) {
            List<SealedProductSaleDto> list = e.getValue();
            if (list == null) {
                continue;
            }
            for (SealedProductSaleDto sale : list) {
                if (sale != null && searchMapId.equals(sale.getSearchMapId())) {
                    String lang = sale.getLanguage();
                    if (lang != null && !lang.isBlank()) {
                        return lang.toLowerCase(Locale.ROOT);
                    }
                    return normalizeLangKey(e.getKey());
                }
            }
        }
        return null;
    }

    private static String normalizeLangKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.toLowerCase(Locale.ROOT);
    }

    private BigDecimal resolveLinePriceUsd(CartItem cartItem, ProductItemDto productItem, Long linePrice) {
        CardProductSaleDto sale = findCardSaleForCartLine(cartItem, productItem);
        if (sale != null && sale.getShowingPriceUsd() != null) {
            return sale.getShowingPriceUsd();
        }
        if (linePrice == null) {
            return null;
        }
        String game = productItem.getCard() != null ? productItem.getCard().getGame() : null;
        return productCalculatingPriceService.calculateProductPriceUsd(linePrice, game);
    }

    private static CardProductSaleDto findCardSaleForCartLine(CartItem cartItem, ProductItemDto productItem) {
        if (productItem.getCard() == null) {
            return null;
        }
        return findCardSaleMatchingLine(
                productItem.getCard().getCardProductSaleDtoMap(),
                cartItem != null ? cartItem.getSearchMapId() : null);
    }

    private CartItemDto.CardProductDto toCardProduct(CartItem cartItem, ProductItemDto productItem) {
        if (productItem.getCard() == null) {
            return null;
        }

        Map<String, List<CardProductSaleDto>> saleMap = productItem.getCard().getCardProductSaleDtoMap();
        if (saleMap == null || saleMap.isEmpty()) {
            return CartItemDto.CardProductDto.builder()
                    .game(productItem.getCard().getGame())
                    .printType(productItem.getCard().getPrintType())
                    .printing(productItem.getCard().getPrinting())
                    .build();
        }

        String language = resolveLineSaleLanguage(productItem, cartItem);
        CardProductSaleDto sale = findCardSaleMatchingLine(saleMap, cartItem != null ? cartItem.getSearchMapId() : null);

        return CartItemDto.CardProductDto.builder()
                .game(productItem.getCard().getGame())
                .condition(sale != null ? sale.getCondition() : null)
                .language(language)
                .printType(productItem.getCard().getPrintType())
                .printing(productItem.getCard().getPrinting())
                .setCode(productItem.getCard().getSetCode())
                .setNumber(productItem.getCard().getSetNumber())
                .setName(unionPriceQueryService.findById(productItem.getCard().getUnionPriceId()).getSetName())
                .build();
    }

    private static CardProductSaleDto findCardSaleMatchingLine(Map<String, List<CardProductSaleDto>> saleMap,
            Long searchMapId) {
        if (saleMap == null || searchMapId == null) {
            return saleMap != null ? firstCardSaleFallback(saleMap) : null;
        }
        for (List<CardProductSaleDto> list : saleMap.values()) {
            if (list == null) {
                continue;
            }
            for (CardProductSaleDto sale : list) {
                if (sale != null && searchMapId.equals(sale.getSearchMapId())) {
                    return sale;
                }
            }
        }
        return firstCardSaleFallback(saleMap);
    }

    private static CardProductSaleDto firstCardSaleFallback(Map<String, List<CardProductSaleDto>> saleMap) {
        return saleMap.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> list.get(0))
                .findFirst()
                .orElse(null);
    }

    private CartItemDto.SuppliesProductDto toSuppliesProduct(ProductItemDto productItem) {
        ProductItemDto.SupplyProductInfoDto supply = productItem.getSupplyProductInfoDto();
        if (supply == null) {
            return null;
        }

        return CartItemDto.SuppliesProductDto.builder()
                .suppliesType(supply.getSuppliesType())
                .table(supply.getTable())
                .tableId(supply.getTableId() != null ? String.valueOf(supply.getTableId()) : null)
                .maker(supply.getMaker())
                .productIp(supply.getProductIp())
                .build();
    }

    private CartItemDto.SealedProductDto toSealedProduct(CartItem cartItem, ProductItemDto productItem) {
        if (productItem.getSealedProductInfoDto() == null) {
            return null;
        }

        String language = resolveLineSaleLanguage(productItem, cartItem);
        if (language == null) {
            language = extractSealedLanguageFallback(productItem.getSealedProductInfoDto().getSealedProductSaleDtoMap());
        }
        return CartItemDto.SealedProductDto.builder()
                .game(productItem.getSealedProductInfoDto().getGame())
                .language(language)
                .build();
    }

    private String extractSealedLanguageFallback(Map<String, List<SealedProductSaleDto>> saleMap) {
        if (saleMap == null || saleMap.isEmpty()) {
            return null;
        }
        return saleMap.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .map(list -> list.get(0))
                .map(SealedProductSaleDto::getLanguage)
                .filter(lang -> lang != null && !lang.isBlank())
                .findFirst()
                .map(lang -> lang.toLowerCase(Locale.ROOT))
                .orElseGet(() -> normalizeLangKey(saleMap.keySet().stream().findFirst().orElse(null)));
    }
}
