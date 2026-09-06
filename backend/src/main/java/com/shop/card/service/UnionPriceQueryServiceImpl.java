package com.shop.card.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.shop.card.entity.UnionPrice;
import com.shop.card.dto.slim.UnionPriceAdminSearchResponseDto;
import com.shop.card.dto.slim.UnionPriceGameSetFacetDto;
import com.shop.card.dto.slim.UnionPriceSearchFacetRowDto;
import com.shop.card.dto.slim.UnionPriceSetFacetDto;
import com.shop.card.dto.slim.UnionPriceSlimDto;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.product.mapper.ProductImageUrlResolver;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnionPriceQueryServiceImpl implements UnionPriceQueryService {

    private final UnionPriceRepository unionPriceRepository;
    private final ProductImageUrlResolver productImageUrlResolver;

    @Override
    public List<UnionPrice> findByGameAndSetNameAndPrintType(String game, String setCodeOrName, String printType) {
        return unionPriceRepository.findForExcelExport(game, setCodeOrName, printType);
    }

    @Override
    public UnionPrice findById(Long id) {
        return unionPriceRepository.findById(id).orElseThrow(() -> new RuntimeException("UnionPrice not found"));
    }

    @Override
    public UnionPriceAdminSearchResponseDto searchByKeywordForAdmin(String keyword, String game, String setCode,
            Pageable pageable) {
        // Step 1: keyword에 "-"가 포함된 경우 setCode + setNumber 정확 일치 검색 시도
        if (keyword != null && keyword.contains("-")) {
            String[] parts = keyword.split("-", 2);
            try {
                long setNumberVal = Long.parseLong(parts[1].trim());
                Page<UnionPriceSlimDto> cards = unionPriceRepository
                        .findBySetCodeAndSetNumber(parts[0].trim(), setNumberVal, normalizeFilter(game), pageable)
                        .map(this::withResolvedImageUrl);
                if (!cards.isEmpty()) {
                    return new UnionPriceAdminSearchResponseDto(cards, List.of());
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Step 2: cardName / cardNameK LIKE 검색 (폴백)
        Page<UnionPriceSlimDto> cards = unionPriceRepository
                .findByCardNameWithFilters(keyword, normalizeFilter(game), normalizeFilter(setCode), pageable)
                .map(this::withResolvedImageUrl);
        List<UnionPriceGameSetFacetDto> facets = buildGameSetFacetsByCardName(keyword);

        return new UnionPriceAdminSearchResponseDto(cards, facets);
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }

    private List<UnionPriceGameSetFacetDto> buildGameSetFacetsByCardName(String keyword) {
        Map<String, List<UnionPriceSetFacetDto>> grouped = new LinkedHashMap<>();

        for (UnionPriceSearchFacetRowDto row : unionPriceRepository.findGameSetFacetsByCardName(keyword)) {
            grouped.computeIfAbsent(row.getGame(), ignored -> new ArrayList<>())
                    .add(new UnionPriceSetFacetDto(row.getSetCode(), row.getSetName()));
        }

        return grouped.entrySet().stream()
                .map(entry -> new UnionPriceGameSetFacetDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private UnionPriceSlimDto withResolvedImageUrl(UnionPriceSlimDto dto) {
        // 1) 이미지 메타(imageSource + imageUrl code)를 최종 표시용 imageUrl로 변환한다.
        String resolvedImageUrl = productImageUrlResolver.resolveImageUrl(
                dto.getImageSource(),
                dto.getGame(),
                dto.getImageUrl());
        // 2) 프론트는 변환된 imageUrl만 소비하도록 동일 필드에 반영한다.
        dto.setImageUrl(resolvedImageUrl);
        // 3) 점진 전환을 위해 imageSource는 유지한다(추후 제거 가능).
        return dto;
    }

}
