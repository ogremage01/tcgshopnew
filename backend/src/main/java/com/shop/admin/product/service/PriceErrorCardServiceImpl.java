package com.shop.admin.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.admin.product.dto.card.CardProductManagementResponseDto;
import com.shop.card.entity.UnionPrice;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.mapper.ProductImageUrlResolver;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.search.repository.map.ProductSearchMapRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceErrorCardServiceImpl implements PriceErrorCardService {

    private final CardProductRepository cardProductRepository;
    private final ProductSearchMapRepository productSearchMapRepository;
    private final ProductImageUrlResolver productImageUrlResolver;

    @Override
    @Transactional
    public void syncPriceErrorVisibility() {
        hideZeroPriceCards();
        restoreNonZeroPriceCards();
    }

    private void hideZeroPriceCards() {
        List<String> toHide = cardProductRepository.findPublicIdsByZeroPriceAndVisible();
        if (toHide.isEmpty()) {
            log.debug("[PriceError] 숨길 카드 없음");
            return;
        }
        int hidden = cardProductRepository.bulkHideZeroPriceLinkedProducts();
        log.info("[PriceError] 가격 오류 카드 비공개 처리: {}건", hidden);
        productSearchMapRepository.hideByCardProductPublicIds(toHide);
        log.info("[PriceError] ProductSearchMap 비공개 처리: {}건", toHide.size());
    }

    private void restoreNonZeroPriceCards() {
        List<String> toRestore = cardProductRepository.findPublicIdsByNonZeroPriceAndHiddenByError();
        if (toRestore.isEmpty()) {
            log.debug("[PriceError] 복원할 카드 없음");
            return;
        }
        int restored = cardProductRepository.bulkRestoreNonZeroPriceLinkedProducts();
        log.info("[PriceError] 가격 정상화 카드 재공개 처리: {}건", restored);
        productSearchMapRepository.restoreByCardProductPublicIds(toRestore);
        log.info("[PriceError] ProductSearchMap 재공개 처리: {}건", toRestore.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardProductManagementResponseDto> getPriceErrorCards(Pageable pageable) {
        return cardProductRepository.findPriceErrorCards(pageable)
                .map(this::toResponseDto);
    }

    private CardProductManagementResponseDto toResponseDto(CardProduct cp) {
        UnionPrice up = cp.getUnionPrice();
        if (up == null) {
            return CardProductManagementResponseDto.builder()
                    .id(cp.getId())
                    .condition(cp.getCondition())
                    .printType(cp.getPrintType())
                    .language(cp.getLanguage())
                    .isVisible(cp.getIsVisible())
                    .isDeleted(cp.getIsDeleted())
                    .currentVisibleStock(cp.getCurrentVisibleStock())
                    .maxVisibleStock(cp.getMaxVisibleStock())
                    .totalStock(cp.getTotalStock())
                    .isAutoUpdatedStock(cp.getIsAutoUpdatedStock())
                    .storageId(cp.getStorageId())
                    .isPriceLinked(cp.getIsPriceLinked())
                    .pricingRate(cp.getPricingRate())
                    .price(cp.getPrice())
                    .memo(cp.getMemo())
                    .build();
        }
        String language = cp.getLanguage();
        String name = "ko".equalsIgnoreCase(language) ? up.getCardNameK() : up.getCardName();
        String imageUrl = productImageUrlResolver.resolveImageUrl(up.getImageSource(), up.getGame(), up.getImageUrl());
        if ("ko".equalsIgnoreCase(language) && imageUrl != null && !imageUrl.isBlank()) {
            imageUrl = imageUrl.replace("/en/", "/ko/");
        }
        return CardProductManagementResponseDto.builder()
                .id(cp.getId())
                .name(name)
                .imageUrl(imageUrl)
                .unionPrice(up)
                .condition(cp.getCondition())
                .printType(cp.getPrintType())
                .language(language)
                .isVisible(cp.getIsVisible())
                .isDeleted(cp.getIsDeleted())
                .currentVisibleStock(cp.getCurrentVisibleStock())
                .maxVisibleStock(cp.getMaxVisibleStock())
                .totalStock(cp.getTotalStock())
                .isAutoUpdatedStock(cp.getIsAutoUpdatedStock())
                .storageId(cp.getStorageId())
                .isPriceLinked(cp.getIsPriceLinked())
                .pricingRate(cp.getPricingRate())
                .price(cp.getPrice())
                .memo(cp.getMemo())
                .build();
    }
}
