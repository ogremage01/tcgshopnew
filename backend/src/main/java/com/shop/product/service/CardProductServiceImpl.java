package com.shop.product.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.product.dto.card.CardProductAdminSearchResponseDto;
import com.shop.admin.product.dto.card.CardProductManagementResponseDto;
import com.shop.card.entity.UnionPrice;
import com.shop.card.dto.slim.UnionPriceGameSetFacetDto;
import com.shop.card.dto.slim.UnionPriceSearchFacetRowDto;
import com.shop.card.dto.slim.UnionPriceSetFacetDto;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.product.dto.card.management.CardProductManagementDto;
import com.shop.product.dto.card.management.CardProductPatchCommand;
import com.shop.product.dto.card.management.CardProductRegisterCommand;
import com.shop.product.dto.card.management.SearchBySetCriteriaDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.mapper.ProductImageUrlResolver;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.card.CardProductRepositoryCustom;
import com.shop.search.event.CardProductBulkChangeEvent;
import com.shop.search.event.CardProductChangeByStorageEvent;
import com.shop.search.event.CardProductChangeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardProductServiceImpl implements CardProductService {

    private final CardProductRepository cardProductRepository;
    private final CardProductRepositoryCustom cardProductRepositoryCustom;
    private final UnionPriceRepository unionPriceRepository;
    private final ProductCalculatingPriceService productCalculatingPriceService;
    private final ProductImageUrlResolver productImageUrlResolver;

    // --------------------------------

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Page<CardProductManagementDto> findByProductIdAndIsDeleted(Long productId, Boolean isDeleted,
            Pageable pageable) {
        return cardProductRepository.findByUnionPriceIdForAdmin(productId, pageable)
                .map(cardProduct -> toManagementDto(cardProduct, cardProduct.getUnionPrice()));
    }

    // 카드 상품 수정
    @Override
    @Transactional
    public CardProductManagementDto patchCardProduct(Long id, CardProductPatchCommand body) {
        CardProduct entity = cardProductRepository.findByIdWithUnionPrice(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "카드 상품을 찾을 수 없습니다."));

        applyPatchBody(entity, body);
        recalculateLinkedPriceIfNeeded(entity);
        applyPriceErrorGuard(entity);

        CardProduct saved = cardProductRepository.save(entity);
        applicationEventPublisher.publishEvent(new CardProductChangeEvent(saved));

        return toManagementDto(saved, saved.getUnionPrice());
    }

    private void recalculateLinkedPriceIfNeeded(CardProduct entity) {
        if (!Boolean.TRUE.equals(entity.getIsPriceLinked())) {
            return;
        }
        UnionPrice unionPrice = entity.getUnionPrice();
        if (unionPrice == null || unionPrice.getPrice() == null) {
            return;
        }
        entity.setCalculatedLinkedPrice(
                productCalculatingPriceService.calculateProductPrice(entity, unionPrice.getPrice()));
    }

    /**
     * 가격 연동(isPriceLinked=true) 상품만 UnionPrice.price=0 을 오류로 본다.
     * 수동 가격(isPriceLinked=false)은 UnionPrice가 0이어도 판매가에 영향이 없으므로 강제 비공개하지 않는다.
     */
    private void applyPriceErrorGuard(CardProduct entity) {
        if (!Boolean.TRUE.equals(entity.getIsPriceLinked())) {
            entity.setHiddenByPriceError(false);
            return;
        }
        UnionPrice up = entity.getUnionPrice();
        boolean isPriceZero = up != null
                && up.getPrice() != null
                && BigDecimal.ZERO.compareTo(up.getPrice()) == 0;
        if (isPriceZero) {
            entity.setIsVisible(false);
            entity.setHiddenByPriceError(true);
        } else {
            entity.setHiddenByPriceError(false);
        }
    }

    // 경우에 따른 데이터 수정 로직
    private void applyPatchBody(CardProduct entity, CardProductPatchCommand body) {
        if (body == null) {
            return;
        }
        if (body.getIsVisible() != null) {
            entity.setIsVisible(body.getIsVisible());
        }
        if (body.getIsPriceLinked() != null) {
            entity.setIsPriceLinked(body.getIsPriceLinked());
        }
        if (body.getStorageId() != null) {
            entity.setStorageId(body.getStorageId());
        }
        if (body.getCurrentVisibleStock() != null) {
            entity.setCurrentVisibleStock(body.getCurrentVisibleStock());
        }
        if (body.getMaxVisibleStock() != null) {
            entity.setMaxVisibleStock(body.getMaxVisibleStock());
        }
        if (body.getTotalStock() != null) {
            entity.setTotalStock(body.getTotalStock());
        }
        if (body.getPricingRate() != null) {
            entity.setPricingRate(body.getPricingRate());
        }
        if (body.getPrice() != null) {
            entity.setPrice(body.getPrice());
        }
        // 메모는 항상 수정
        entity.setMemo(body.getMemo());
    }

    private CardProductManagementDto toManagementDto(CardProduct cardProduct,
            UnionPrice unionPrice) {
        if (unionPrice == null) {
            return CardProductManagementDto.builder()
                    .id(cardProduct.getId())
                    .name(null)
                    .imageUrl(null)
                    .unionPrice(null)
                    .condition(cardProduct.getCondition())
                    .printType(cardProduct.getPrintType())
                    .language(cardProduct.getLanguage())
                    .isVisible(cardProduct.getIsVisible())
                    .isDeleted(cardProduct.getIsDeleted())
                    .currentVisibleStock(cardProduct.getCurrentVisibleStock())
                    .maxVisibleStock(cardProduct.getMaxVisibleStock())
                    .totalStock(cardProduct.getTotalStock())
                    .isAutoUpdatedStock(cardProduct.getIsAutoUpdatedStock())
                    .storageId(cardProduct.getStorageId())
                    .isPriceLinked(cardProduct.getIsPriceLinked())
                    .pricingRate(cardProduct.getPricingRate())
                    .price(cardProduct.getPrice())
                    .memo(cardProduct.getMemo())
                    .build();
        }

        final String language = cardProduct.getLanguage();
        final String cardName = "ko".equalsIgnoreCase(language)
                ? unionPrice.getCardNameK()
                : unionPrice.getCardName();
        final String imageUrl = resolveImageUrl(unionPrice, language);
        return CardProductManagementDto.builder()
                .id(cardProduct.getId())
                .name(cardName)
                .imageUrl(imageUrl)
                .unionPrice(unionPrice)
                .condition(cardProduct.getCondition())
                .printType(cardProduct.getPrintType())
                .language(cardProduct.getLanguage())
                .isVisible(cardProduct.getIsVisible())
                .isDeleted(cardProduct.getIsDeleted())
                .currentVisibleStock(cardProduct.getCurrentVisibleStock())
                .maxVisibleStock(cardProduct.getMaxVisibleStock())
                .totalStock(cardProduct.getTotalStock())
                .isAutoUpdatedStock(cardProduct.getIsAutoUpdatedStock())
                .storageId(cardProduct.getStorageId())
                .isPriceLinked(cardProduct.getIsPriceLinked())
                .pricingRate(cardProduct.getPricingRate())

                .price(cardProduct.getPrice())
                .memo(cardProduct.getMemo())
                .build();
    }

    private String resolveImageUrl(UnionPrice unionPrice, String language) {
        String imageUrl = productImageUrlResolver.resolveImageUrl(
                unionPrice.getImageSource(),
                unionPrice.getGame(),
                unionPrice.getImageUrl());
        if ("ko".equalsIgnoreCase(language) && imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.replace("/en/", "/ko/");
        }
        return imageUrl;
    }

    @Override
    @Transactional
    public void registerCardProduct(CardProductRegisterCommand cardProductRegisterDto) {
        Long unionPriceId = cardProductRegisterDto.getUnionPriceId();
        if (unionPriceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UnionPrice ID가 필요합니다.");
        }
        UnionPrice unionPriceForValidation = unionPriceRepository.findById(unionPriceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "UnionPrice를 찾을 수 없습니다."));

        String language = cardProductRegisterDto.getLanguage();
        if (language != null && language.equalsIgnoreCase("ko")) {
            if (unionPriceForValidation.getCardNameK() == null || unionPriceForValidation.getCardNameK().isBlank()) {
                log.error("한글 카드가 존재하지 않습니다. unionPriceId: {}", unionPriceId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한글 카드가 존재하지 않습니다.");
            }
        }
        if (language == null || !language.equalsIgnoreCase("ko")) {
            if (unionPriceForValidation.getCardName() == null || unionPriceForValidation.getCardName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "영어 카드가 존재하지 않습니다.");
            }
        }

        CardProduct cardProduct = cardProductRegisterDto.toEntity();
        cardProduct.setUnionPrice(unionPriceForValidation);
        if (cardProduct.getCurrentVisibleStock() > cardProduct.getTotalStock()) {
            cardProduct.setCurrentVisibleStock(cardProduct.getTotalStock());
        }
        if (Boolean.TRUE.equals(cardProduct.getIsPriceLinked())) {
            cardProduct.setCalculatedLinkedPrice(
                    productCalculatingPriceService.calculateProductPrice(cardProduct,
                            unionPriceForValidation.getPrice()));
        }
        applyPriceErrorGuard(cardProduct);
        CardProduct saved = cardProductRepository.save(cardProduct);
        applicationEventPublisher.publishEvent(new CardProductChangeEvent(saved));

    }

    @Override
    @EntityGraph(attributePaths = "unionPrice")
    public Page<CardProductManagementDto> findByProductNameOrCodeNumber(
            String keyword,
            String game,
            String setCode,
            Pageable pageable) {
        return cardProductRepository.findByKeywordForAdmin(
                        keyword,
                        normalizeFilter(game),
                        normalizeFilter(setCode),
                        pageable)
                .map(cardProduct -> toManagementDto(cardProduct, cardProduct.getUnionPrice()));
    }

    @Override
    public List<UnionPriceGameSetFacetDto> findGameSetFacetsByKeywordForAdmin(String keyword) {
        Map<String, List<UnionPriceSetFacetDto>> grouped = new LinkedHashMap<>();

        for (UnionPriceSearchFacetRowDto row : cardProductRepository.findGameSetFacetsByKeywordForAdmin(keyword)) {
            grouped.computeIfAbsent(row.getGame(), ignored -> new ArrayList<>())
                    .add(new UnionPriceSetFacetDto(row.getSetCode(), row.getSetName()));
        }

        return grouped.entrySet().stream()
                .map(entry -> new UnionPriceGameSetFacetDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    @EntityGraph(attributePaths = "unionPrice")
    public CardProductAdminSearchResponseDto searchByKeywordForAdmin(
            String keyword, String game, String setCode, Pageable pageable) {
        // Step 1: keyword에 "-"가 포함된 경우 setCode + setNumber 정확 일치 검색 시도
        if (keyword != null && keyword.contains("-")) {
            String[] parts = keyword.split("-", 2);
            try {
                long setNumberVal = Long.parseLong(parts[1].trim());
                Page<CardProductManagementResponseDto> cards = cardProductRepository
                        .findBySetCodeAndSetNumberForAdmin(parts[0].trim(), setNumberVal, normalizeFilter(game),
                                pageable)
                        .map(c -> CardProductManagementResponseDto.from(toManagementDto(c, c.getUnionPrice())));
                if (!cards.isEmpty()) {
                    return new CardProductAdminSearchResponseDto(cards, List.of());
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Step 2: cardName / cardNameK LIKE 검색 (폴백)
        Page<CardProductManagementResponseDto> cards = cardProductRepository
                .findByCardNameForAdmin(keyword, normalizeFilter(game), normalizeFilter(setCode), pageable)
                .map(c -> CardProductManagementResponseDto.from(toManagementDto(c, c.getUnionPrice())));
        List<UnionPriceGameSetFacetDto> facets = buildGameSetFacetsByCardName(keyword);
        return new CardProductAdminSearchResponseDto(cards, facets);
    }

    private List<UnionPriceGameSetFacetDto> buildGameSetFacetsByCardName(String keyword) {
        Map<String, List<UnionPriceSetFacetDto>> grouped = new LinkedHashMap<>();

        for (UnionPriceSearchFacetRowDto row : cardProductRepository.findGameSetFacetsByCardNameForAdmin(keyword)) {
            grouped.computeIfAbsent(row.getGame(), ignored -> new ArrayList<>())
                    .add(new UnionPriceSetFacetDto(row.getSetCode(), row.getSetName()));
        }

        return grouped.entrySet().stream()
                .map(entry -> new UnionPriceGameSetFacetDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }

    @Override
    public Page<CardProductManagementDto> findByProductIdInForAdmin(List<Long> cardIdList, Pageable pageable) {
        return cardProductRepository.findByProductIdInVisibleAndIsNotDeleted(cardIdList, pageable)
                .map(cardProduct -> toManagementDto(cardProduct, cardProduct.getUnionPrice()));
    }

    // 수정중
    @Override
    @Transactional
    public int registerOrUpdateCardProducts(List<CardProduct> cardProductList) throws IOException {
        int updatedCount = 0;
        var pricingContext = productCalculatingPriceService.loadPricingContext();
        for (CardProduct cardProduct : cardProductList) {
            Long unionPriceId = cardProduct.getUnionPrice().getId();
            Boolean isVisible = cardProduct.getIsVisible();
            Boolean isDeleted = false;
            Long storageId = cardProduct.getStorageId();
            Boolean isPriceLinked = cardProduct.getIsPriceLinked();
            Double pricingRate = cardProduct.getPricingRate();
            String language = cardProduct.getLanguage();
            String condition = cardProduct.getCondition();
            String printType = cardProduct.getPrintType();

            log.debug(
                    "findByPattern 검색 조건 - unionPriceId: {}, isVisible: {}, isDeleted: {}, storageId: {}, isPriceLinked: {}, pricingRate: {}, language: {}, condition: {}, printType: {}",
                    unionPriceId, isVisible, isDeleted, storageId, isPriceLinked, pricingRate, language, condition,
                    printType);

            CardProduct existingCardProduct = cardProductRepository
                    .findByPattern(unionPriceId, isVisible, isDeleted, storageId, isPriceLinked, pricingRate, language,
                            condition, printType)
                    .orElse(null);
            if (existingCardProduct != null) {
                log.info("기존 CardProduct 발견 - id: {}, unionPriceId: {}, language: {}, condition: {}, printType: {}",
                        existingCardProduct.getId(), unionPriceId, language, condition, printType);
                cardProduct.setId(existingCardProduct.getId());
                cardProduct.setTotalStock(existingCardProduct.getTotalStock() + cardProduct.getTotalStock());
                updatedCount++;
            } else {
                log.debug(
                        "기존 CardProduct 없음 - 새로운 상품으로 생성될 예정. unionPriceId: {}, language: {}, condition: {}, printType: {}",
                        unionPriceId, language, condition, printType);
            }
            if (Boolean.TRUE.equals(cardProduct.getIsPriceLinked())) {
                cardProduct.setCalculatedLinkedPrice(
                        productCalculatingPriceService.calculateProductPrice(
                                cardProduct,
                                cardProduct.getUnionPrice().getPrice(),
                                pricingContext));
            }
            if (cardProduct.getCurrentVisibleStock() > cardProduct.getTotalStock()) {
                cardProduct.setCurrentVisibleStock(cardProduct.getTotalStock());
            }
            applyPriceErrorGuard(cardProduct);
        }
        cardProductRepository.saveAll(cardProductList);
        applicationEventPublisher.publishEvent(new CardProductBulkChangeEvent(cardProductList));
        log.info("카드 제품 등록 또는 수정 완료: {}건", cardProductList.size());
        return updatedCount;
    }

    // 보관소 삭제 시 보관 중이던 카드 제품 삭제 지만 이를 우려해서 삭제 기능 제거함.
    @Override
    @Transactional
    public void deleteCardProductsByStorageId(Long storageId) {
        cardProductRepository.updateByStorageIdAndIsDeleted(storageId);

        applicationEventPublisher.publishEvent(new CardProductChangeByStorageEvent(storageId));
    }

    // 카드 제품 단건 삭제
    @Override
    @Transactional
    public void deleteCardProductById(Long id) {
        cardProductRepository.softDeleteCardProductById(id);
        applicationEventPublisher.publishEvent(new CardProductChangeEvent(
                cardProductRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "카드 상품을 찾을 수 없습니다."))));
    }

    @Override
    public Page<CardProductManagementDto> searchBySetForAdmin(SearchBySetCriteriaDto criteria, Pageable pageable) {
        return cardProductRepositoryCustom.findBySetForAdmin(criteria, pageable)
                .map(cardProduct -> toManagementDto(cardProduct, cardProduct.getUnionPrice()));
    }

}
