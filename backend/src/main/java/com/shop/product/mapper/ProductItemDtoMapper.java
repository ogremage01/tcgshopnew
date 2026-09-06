package com.shop.product.mapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.shop.card.entity.UnionPrice;
import com.shop.card.metadata.support.PrintingResolver;
import com.shop.product.dto.GameSalesInfoDto;
import com.shop.product.dto.ProductItemDto;
import com.shop.product.dto.ProductItemDto.CardProductInfoDto;
import com.shop.product.dto.ProductItemDto.ManualProductInfoDto;
import com.shop.product.dto.ProductItemDto.SealedProductInfoDto;
import com.shop.product.dto.SealedProductSaleDto;
import com.shop.product.dto.card.CardProductSaleDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.service.supply.SuppliesTypeLabelResolver;
import com.shop.product.service.ProductCalculatingPriceService;
import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.service.RewardRuleMatchService;
import com.shop.search.dto.enums.ProductTableEnum;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductItemDtoMapper {

    // ------------------------------------------------
    // 상품 정보 매핑
    // ------------------------------------------------

    private final ProductCalculatingPriceService productCalculatingPriceService;
    private final ProductImageUrlResolver productImageUrlResolver;
    private final RewardRuleMatchService rewardRuleMatchService;
    private final SuppliesTypeLabelResolver suppliesTypeLabelResolver;
    private static final List<String> LANGUAGE_CODES = List.of(
            "en", "ko", "ge", "sp", "fr", "it", "ja", "po", "ru", "cs", "ct");
    private static final String PRODUCT_TYPE_CARDS = "Cards";
    private static final String PRODUCT_TYPE_SEALED_PRODUCTS = "SealedProducts";
    private static final String PRODUCT_TYPE_MANUAL_PRODUCTS = "ManualProducts";
    private static final String PRODUCT_TYPE_SUPPLIES = "Supplies";

    // 카드 상품을 조회하여 ProductItemDto를 반환
    public ProductItemDto fromCardProducts(UnionPrice unionPrice, List<CardProduct> offersForSameUnionPrice,
            String primaryProductId, Map<String, Long> searchMapIdByProductId, GameSalesInfoDto gameSalesInfo) {
        // STEP 1) 기준 데이터가 없으면 즉시 종료
        if (unionPrice == null) {
            return null;
        }

        // STEP 2) 대표 상품(primary) 선택
        // - primaryProductId가 있으면 해당 상품 우선
        // - 없으면 목록 첫 번째 상품을 대표로 사용
        CardProduct primary = offersForSameUnionPrice.stream()
                .filter(p -> primaryProductId != null && primaryProductId.equals(p.getPublicId()))
                .findFirst()
                .orElse(offersForSameUnionPrice.isEmpty() ? null : offersForSameUnionPrice.get(0));

        // STEP 3) 대표 상품이 없으면 매핑 불가
        if (primary == null) {
            return null;
        }

        // STEP 4) 상품 타입 정규화 (분기 기준 통일)
        String normalizedProductType = normalizeProductType(unionPrice.getProductType());
        boolean isSealedProduct = PRODUCT_TYPE_SEALED_PRODUCTS.equals(normalizedProductType);

        // STEP 5) if 블록 밖에서 선언해서 스코프 문제 방지
        Map<String, List<CardProductSaleDto>> cardProductSaleDtoMap = null;
        Map<String, List<SealedProductSaleDto>> sealedProductSaleDtoMap = null;

        // STEP 6) 상품 타입별 판매 정보 맵 구성
        if (PRODUCT_TYPE_CARDS.equals(normalizedProductType)) {
            cardProductSaleDtoMap = offersForSameUnionPrice.stream()
                    .filter(cp -> cp.getLanguage() != null)
                    .collect(Collectors.groupingBy(
                            cp -> normalizeLanguage(cp.getLanguage()),
                            Collectors.mapping(cp -> toSaleDto(cp, searchMapIdByProductId), Collectors.toList())));
        } else if (PRODUCT_TYPE_SEALED_PRODUCTS.equals(normalizedProductType)) {
            sealedProductSaleDtoMap = offersForSameUnionPrice.stream()
                    .filter(cp -> cp.getLanguage() != null)
                    .collect(Collectors.groupingBy(
                            cp -> normalizeLanguage(cp.getLanguage()),
                            Collectors.mapping(cp -> toSealedSaleDto(cp, searchMapIdByProductId),
                                    Collectors.toList())));
        }

        // STEP 7) 표시 가격 계산
        Long displayPrice = resolveShowingPrice(primary);

        // STEP 8) 공통 + 타입별 필드를 한 DTO로 반환
        CardProductInfoDto cardInfo = isSealedProduct ? null
                : CardProductInfoDto.builder()
                        .game(unionPrice.getGame())
                        .cardProductSaleDtoMap(cardProductSaleDtoMap)
                        .rarity(unionPrice.getRarity())
                        .printType(unionPrice.getPrintType())
                        .printing(resolvePrinting(unionPrice))
                        .setCode(unionPrice.getSetCode())
                        .setNumber(extractSetNumber(unionPrice.getCheckCodeRefined()))
                        .setName(unionPrice.getSetName())
                        .gameSalesInfo(gameSalesInfo)
                        .build();
        applyUnionPriceIdentifier(cardInfo, unionPrice);

        Boolean isDoubleSided = unionPrice.getIsDoubleSided();
        var itemBuilder = ProductItemDto.builder()
                .productType(normalizedProductType)
                .productNameEn(unionPrice.getCardName())
                .productNameKo(unionPrice.getCardNameK())
                .imageUrlEn(resolveImageUrl(unionPrice))
                .imageUrlKo(resolveImageUrlKo(unionPrice))
                .languageImageUrlMap(resolveLanguageImageUrlMap(unionPrice))
                .isDoubleSided(isDoubleSided);
        if (Boolean.TRUE.equals(isDoubleSided)) {
            itemBuilder
                    .backImageUrlEn(resolveImageUrlBack(unionPrice))
                    .backImageUrlKo(resolveImageUrlBackKo(unionPrice))
                    .languageBackImageUrlMap(resolveLanguageBackImageUrlMap(unionPrice));
        }
        ProductItemDto dto = itemBuilder
                .price(displayPrice)
                .setName(unionPrice.getSetName())
                .setCode(unionPrice.getSetCode())
                .card(cardInfo)
                .table(ProductTableEnum.UNION_PRICE.name())
                .tableId(unionPrice.getId())
                .currentVisibleStock(primary.getCurrentVisibleStock())
                .sealedProductInfoDto(isSealedProduct ? SealedProductInfoDto.builder()
                        .game(unionPrice.getGame())
                        .sealedProductSaleDtoMap(sealedProductSaleDtoMap)
                        .gameSalesInfo(gameSalesInfo)
                        .build() : null)
                .build();

        ProductMatchContext context = ProductMatchContext.builder()
                .game(unionPrice.getGame())
                .productType(normalizedProductType)
                .condition(primary.getCondition())
                .language(primary.getLanguage())
                .set(unionPrice.getSetCode())
                .rarity(unionPrice.getRarity())
                .printing(unionPrice.getPrintType())
                .cardName(unionPrice.getCardName())
                .setNumber(unionPrice.getCheckCodeRefined() != null
                        ? extractSetNumber(unionPrice.getCheckCodeRefined())
                        : null)
                .build();
        applyRewardFields(dto, context);
        return dto;
    }

    // 목록/검색용 경량 매핑 (상세 전용 정보 제외)
    public ProductItemDto fromCardProducts(UnionPrice unionPrice, List<CardProduct> offersForSameUnionPrice,
            String primaryProductId, Map<String, Long> searchMapIdByProductId) {
        return fromCardProducts(unionPrice, offersForSameUnionPrice, primaryProductId, searchMapIdByProductId, null);
    }

    private void applyUnionPriceIdentifier(CardProductInfoDto cardInfo, UnionPrice unionPrice) {
        if (cardInfo == null || unionPrice == null) {
            return;
        }
        cardInfo.setUnionPriceId(unionPrice.getId());
        String publicId = unionPrice.getPublicId();
        cardInfo.setPublicId(
                (publicId == null || publicId.isBlank()) ? String.valueOf(unionPrice.getId()) : publicId);
    }

    /** 표시용 printing. 세트별 override 후, 비어 있으면 printType으로 대체 */
    private String resolvePrinting(UnionPrice unionPrice) {
        return PrintingResolver.resolve(unionPrice);
    }

    // CardProduct를 CardProductSaleDto로 변환
    private CardProductSaleDto toSaleDto(CardProduct cp, Map<String, Long> searchMapIdByProductId) {
        Long showingPrice = resolveShowingPrice(cp);
        String game = cp.getUnionPrice() != null ? cp.getUnionPrice().getGame() : null;
        return CardProductSaleDto.builder()
                .id(cp.getId())
                // searchMapIdByProductId에서 찾은 검색 매핑 ID를 설정
                .searchMapId(searchMapIdByProductId.get(cp.getPublicId()))
                .condition(cp.getCondition())
                .currentVisibleStock(cp.getCurrentVisibleStock())
                .showingPrice(showingPrice)
                .showingPriceUsd(resolveShowingPriceUsd(showingPrice, game))
                .build();
    }

    private SealedProductSaleDto toSealedSaleDto(CardProduct cp, Map<String, Long> searchMapIdByProductId) {
        return SealedProductSaleDto.builder()
                .id(cp.getId())
                .searchMapId(searchMapIdByProductId.get(cp.getPublicId()))
                .language(cp.getLanguage())
                .currentVisibleStock(cp.getCurrentVisibleStock())
                .showingPrice(resolveShowingPrice(cp))
                .build();
    }

    // CardProduct의 표시 가격을 반환
    private Long resolveShowingPrice(CardProduct p) {
        UnionPrice u = p.getUnionPrice();
        // 가격 연동 사용여부가 true이고 연동계산 된 가격이 있으면 연동계산 된 가격을 반환
        if (Boolean.TRUE.equals(p.getIsPriceLinked())) {
            if (p.getCalculatedLinkedPrice() != null) {
                return p.getCalculatedLinkedPrice();
            }
            // 가격 연동 사용여부가 true이고 연동계산 된 가격이 없으면 연동계산 된 가격을 계산
            if (u != null && u.getPrice() != null) {
                return productCalculatingPriceService.calculateProductPrice(p, u.getPrice());
            }
        }
        // 가격 연동 사용여부가 false이거나 연동계산 된 가격이 없으면 설정 가격을 반환
        if (p.getPrice() != null) {
            return p.getPrice();
        }
        return 0L;
    }

    private BigDecimal resolveShowingPriceUsd(Long krwPrice, String game) {
        return productCalculatingPriceService.calculateProductPriceUsd(krwPrice, game);
    }

    // UnionPrice의 이미지 URL을 반환
    private String resolveImageUrl(UnionPrice unionPrice) {
        return productImageUrlResolver.resolveImageUrl(
                unionPrice.getImageSource(),
                unionPrice.getGame(),
                unionPrice.getImageUrl());
    }

    private String resolveImageUrlKo(UnionPrice unionPrice) {
        String url = productImageUrlResolver.resolveImageUrl(
                unionPrice.getImageSource(),
                unionPrice.getGame(),
                unionPrice.getImageUrl());
        if (url.isEmpty()) {
            return url;
        }
        return url.replace("-en.png", "-ko.png");
    }

    private Map<String, String> resolveLanguageImageUrlMap(UnionPrice unionPrice) {
        String enUrl = resolveImageUrl(unionPrice);
        Map<String, String> result = new LinkedHashMap<>();
        boolean keepEnglish = isScryfallSource(unionPrice);
        for (String language : LANGUAGE_CODES) {
            result.put(language, keepEnglish ? enUrl : resolveLanguageImageUrl(enUrl, language, false));
        }
        return result;
    }

    private String resolveImageUrlBack(UnionPrice unionPrice) {
        return productImageUrlResolver.resolveImageUrlBack(
                unionPrice.getImageSource(),
                unionPrice.getGame(),
                unionPrice.getImageUrl());
    }

    private String resolveImageUrlBackKo(UnionPrice unionPrice) {
        String url = productImageUrlResolver.resolveImageUrlBack(
                unionPrice.getImageSource(),
                unionPrice.getGame(),
                unionPrice.getImageUrl());
        if (url.isEmpty() || isScryfallSource(unionPrice)) {
            return url;
        }
        return url.replace("-enback", "-koback");
    }

    private Map<String, String> resolveLanguageBackImageUrlMap(UnionPrice unionPrice) {
        String enUrl = resolveImageUrlBack(unionPrice);
        Map<String, String> result = new LinkedHashMap<>();
        boolean keepEnglish = isScryfallSource(unionPrice);
        for (String language : LANGUAGE_CODES) {
            result.put(language, keepEnglish ? enUrl : resolveLanguageImageUrl(enUrl, language, true));
        }
        return result;
    }

    private static boolean isScryfallSource(UnionPrice unionPrice) {
        return unionPrice != null && "SCRYFALL".equalsIgnoreCase(unionPrice.getImageSource());
    }

    private String resolveLanguageImageUrl(String enUrl, String language, boolean backSide) {
        if (enUrl == null || enUrl.isBlank() || language == null || language.isBlank() || "en".equals(language)) {
            return enUrl;
        }
        if (backSide && enUrl.contains("-enback")) {
            return enUrl.replace("-enback", "-" + language + "back");
        }
        if (enUrl.contains("-en.")) {
            return enUrl.replace("-en.", "-" + language + ".");
        }
        if (enUrl.contains("-en/")) {
            return enUrl.replace("-en/", "-" + language + "/");
        }
        return enUrl;
    }

    private String normalizeProductType(String rawProductType) {
        if (rawProductType == null || rawProductType.isBlank()) {
            return PRODUCT_TYPE_CARDS;
        }
        String normalized = rawProductType.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("sealed")) {
            return PRODUCT_TYPE_SEALED_PRODUCTS;
        }
        if (normalized.startsWith("cards")) {
            return PRODUCT_TYPE_CARDS;
        }
        return rawProductType;
    }

    private String normalizeLanguage(String rawLanguage) {
        return rawLanguage == null ? "" : rawLanguage.trim().toLowerCase(Locale.ROOT);
    }

    private void applyRewardFields(ProductItemDto dto, ProductMatchContext context) {
        if (dto == null || dto.getPrice() == null) {
            return;
        }
        rewardRuleMatchService.match(context, dto.getPrice()).ifPresent(result -> {
            dto.setRewardPercentage(result.getRewardPercentage());
            dto.setSaveAmount(result.getSaveAmount());
            dto.setMatchedRuleId(result.getRuleId());
        });
    }

    private String extractSetNumber(String checkCodeRefined) {
        String[] elements = checkCodeRefined.split("-");
        // MTG의 The List 예외처리
        if (elements[0].toLowerCase(Locale.ROOT).contains("plst")) {
            return elements[1] + "-" + elements[2];
        }
        return elements[1];
    }

    /**
     * CardProduct 없이 UnionPrice만으로 ProductItemDto를 생성한다.
     * 재고 없음, 판매 옵션 없음 상태이며 가격은 NM 등급 + 환율 기준으로 계산된다.
     */
    public ProductItemDto fromUnionPriceOnly(UnionPrice unionPrice, String primaryProductId) {
        return fromUnionPriceOnly(unionPrice, primaryProductId, null);
    }

    /**
     * 상세 조회 등에서 {@link GameSalesInfoDto}를 함께 실을 때 사용한다.
     */
    public ProductItemDto fromUnionPriceOnly(UnionPrice unionPrice, String primaryProductId,
            GameSalesInfoDto gameSalesInfo) {
        if (unionPrice == null) {
            return null;
        }
        String normalizedProductType = normalizeProductType(unionPrice.getProductType());
        boolean isSealedProduct = PRODUCT_TYPE_SEALED_PRODUCTS.equals(normalizedProductType);

        Long displayPrice = productCalculatingPriceService.calculatePriceFromUnionPrice(unionPrice);

        // CardProduct 없음을 나타내는 기본 EN/NM 항목 (id=null → 장바구니 불가)
        CardProductSaleDto defaultSale = CardProductSaleDto.builder()
                .condition("NM")
                .currentVisibleStock(0L)
                .showingPrice(displayPrice)
                .showingPriceUsd(resolveShowingPriceUsd(displayPrice, unionPrice.getGame()))
                .build();
        Map<String, List<CardProductSaleDto>> defaultSaleDtoMap = isSealedProduct
                ? Map.of()
                : Map.of("en", List.of(defaultSale));

        CardProductInfoDto cardInfo = isSealedProduct ? null
                : CardProductInfoDto.builder()
                        .game(unionPrice.getGame())
                        .cardProductSaleDtoMap(defaultSaleDtoMap)
                        .rarity(unionPrice.getRarity())
                        .printType(unionPrice.getPrintType())
                        .printing(resolvePrinting(unionPrice))
                        .setCode(unionPrice.getSetCode())
                        .setNumber(extractSetNumber(unionPrice.getCheckCodeRefined()))
                        .setName(unionPrice.getSetName())
                        .gameSalesInfo(gameSalesInfo)
                        .build();
        applyUnionPriceIdentifier(cardInfo, unionPrice);

        Boolean isDoubleSided = unionPrice.getIsDoubleSided();
        var itemBuilder = ProductItemDto.builder()
                .productType(normalizedProductType)
                .productNameEn(unionPrice.getCardName())
                .productNameKo(unionPrice.getCardNameK())
                .imageUrlEn(resolveImageUrl(unionPrice))
                .imageUrlKo(resolveImageUrlKo(unionPrice))
                .languageImageUrlMap(resolveLanguageImageUrlMap(unionPrice))
                .isDoubleSided(isDoubleSided);
        if (Boolean.TRUE.equals(isDoubleSided)) {
            itemBuilder
                    .backImageUrlEn(resolveImageUrlBack(unionPrice))
                    .backImageUrlKo(resolveImageUrlBackKo(unionPrice))
                    .languageBackImageUrlMap(resolveLanguageBackImageUrlMap(unionPrice));
        }
        ProductItemDto dto = itemBuilder
                .price(displayPrice)
                .setName(unionPrice.getSetName())
                .setCode(unionPrice.getSetCode())
                .card(cardInfo)
                .table(ProductTableEnum.UNION_PRICE.name())
                .tableId(unionPrice.getId())
                .currentVisibleStock(0L)
                .sealedProductInfoDto(isSealedProduct ? SealedProductInfoDto.builder()
                        .game(unionPrice.getGame())
                        .sealedProductSaleDtoMap(Map.of())
                        .gameSalesInfo(gameSalesInfo)
                        .build() : null)
                .build();

        ProductMatchContext context = ProductMatchContext.builder()
                .game(unionPrice.getGame())
                .productType(normalizedProductType)
                .set(unionPrice.getSetCode())
                .rarity(unionPrice.getRarity())
                .printing(unionPrice.getPrintType())
                .cardName(unionPrice.getCardName())
                .setNumber(unionPrice.getCheckCodeRefined() != null
                        ? extractSetNumber(unionPrice.getCheckCodeRefined())
                        : null)
                .build();
        applyRewardFields(dto, context);
        return dto;
    }

    public ProductItemDto fromManualProduct(Long searchMapId, ManualProduct manualProduct) {
        ProductItemDto dto = ProductItemDto.builder()
                .productType(PRODUCT_TYPE_MANUAL_PRODUCTS)
                .productNameEn(manualProduct.getNameEn())
                .productNameKo(manualProduct.getNameKo())
                .imageUrlEn(manualProduct.getImgUrl())
                .imageUrlKo(manualProduct.getImgUrl())
                .languageImageUrlMap(resolveManualLanguageImageUrlMap(manualProduct.getImgUrl()))
                .price(manualProduct.getPrice())
                .currentVisibleStock(manualProduct.getStock())
                .productIp(manualProduct.getProductIp())
                .manualProductInfoDto(ManualProductInfoDto.builder()
                        .productType(manualProduct.getProductType())
                        .searchMapId(searchMapId)
                        .publicId(manualProduct.getPublicId())
                        .description(manualProduct.getDescription())
                        .build())
                .table(ProductTableEnum.MANUAL_PRODUCT.name())
                .tableId(manualProduct.getId())
                .build();

        ProductMatchContext context = ProductMatchContext.builder()
                .productType(PRODUCT_TYPE_MANUAL_PRODUCTS)
                .build();
        applyRewardFields(dto, context);
        return dto;
    }

    public ProductItemDto fromSealedProduct(Long searchMapId, SealedProduct sealedProduct) {
        ProductItemDto dto = ProductItemDto.builder()
                .productType(PRODUCT_TYPE_SEALED_PRODUCTS)
                .productNameEn(sealedProduct.getProductNameEn())
                .productNameKo(sealedProduct.getProductNameKo())
                .imageUrlEn(sealedProduct.getImageUrl())
                .imageUrlKo(sealedProduct.getImageUrl())
                .languageImageUrlMap(resolveManualLanguageImageUrlMap(sealedProduct.getImageUrl()))
                .price(sealedProduct.getPrice())
                .setName(sealedProduct.getSetName())
                .setCode(sealedProduct.getSetCode())
                .currentVisibleStock(sealedProduct.visibleStock())
                .productIp(sealedProduct.getGame())
                .table(ProductTableEnum.SEALED_PRODUCT.name())
                .tableId(sealedProduct.getId())
                .sealedProductInfoDto(SealedProductInfoDto.builder()
                        .game(sealedProduct.getGame())
                        .publicId(sealedProduct.getPublicId())
                        .sealedProductSaleDtoMap(Map.of(
                                normalizeLanguage(sealedProduct.getLanguage()),
                                List.of(SealedProductSaleDto.builder()
                                        .id(sealedProduct.getId())
                                        .searchMapId(searchMapId)
                                        .language(sealedProduct.getLanguage())
                                        .currentVisibleStock(sealedProduct.visibleStock())
                                        .showingPrice(sealedProduct.getPrice() == null ? 0L : sealedProduct.getPrice())
                                        .build())))
                        .build())
                .build();

        ProductMatchContext context = ProductMatchContext.builder()
                .game(sealedProduct.getGame())
                .productType(PRODUCT_TYPE_SEALED_PRODUCTS)
                .set(sealedProduct.getSetCode())
                .build();
        applyRewardFields(dto, context);
        return dto;
    }

    public ProductItemDto fromSupply(Long searchMapId, Supply supply) {
        var suppliesTypeFacet = suppliesTypeLabelResolver.resolveFacet(supply.getSupplyType());
        ProductItemDto dto = ProductItemDto.builder()
                .productType(PRODUCT_TYPE_SUPPLIES)
                .productNameEn(supply.getNameEn())
                .productNameKo(supply.getNameKo())
                .imageUrlEn(supply.getImgUrl())
                .imageUrlKo(supply.getImgUrl())
                .languageImageUrlMap(resolveManualLanguageImageUrlMap(supply.getImgUrl()))
                .price(supply.getPrice())
                .currentVisibleStock(supply.getStock())
                .supplyProductInfoDto(ProductItemDto.SupplyProductInfoDto.builder()
                        .searchMapId(searchMapId)
                        .publicId(supply.getPublicId())
                        .suppliesType(supply.getSupplyType())
                        .suppliesTypeNameEn(suppliesTypeFacet != null ? suppliesTypeFacet.getNameEn() : supply.getSupplyType())
                        .suppliesTypeNameKo(suppliesTypeFacet != null ? suppliesTypeFacet.getNameKo() : supply.getSupplyType())
                        .table(ProductTableEnum.SUPPLY.name())
                        .tableId(supply.getId())
                        .maker(supply.getMaker())
                        .description(supply.getDescription())
                        .build())
                .build();

        ProductMatchContext context = ProductMatchContext.builder()
                .productType(PRODUCT_TYPE_SUPPLIES)
                .build();
        applyRewardFields(dto, context);
        return dto;
    }

    private Map<String, String> resolveManualLanguageImageUrlMap(String imageUrl) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("en", imageUrl);
        result.put("ko", imageUrl);
        return result;
    }

}
