package com.shop.product.service.sealedProduct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.shop.common.fileUpload.service.FileUploadService;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.product.dto.sealed.AddSealedProductDto;
import com.shop.product.dto.sealed.SealedProductDto;
import com.shop.product.dto.sealed.UpdateSealedProductDto;
import com.shop.product.dto.sealed.SealedProductGameFacetDto;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.enums.GameEnum;
import com.shop.product.repository.card.CardProductLanguageRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.search.event.SealedProductChangeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SealedProductServiceImpl implements SealedProductService {

    private final SealedProductRepository sealedProductRepository;
    private final FileUploadService fileUploadService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CardProductLanguageRepository cardProductLanguageRepository;
    private final OfflineProductRepository offlineProductRepository;

    @Override
    @Transactional
    public void createSealedProduct(AddSealedProductDto dto) throws IOException {
        log.info("[SealedProduct] createSealedProduct 진입 - 파일명={}, 크기={}bytes, contentType={}",
                dto.getImageFile() != null ? dto.getImageFile().getOriginalFilename() : "null",
                dto.getImageFile() != null ? dto.getImageFile().getSize() : -1,
                dto.getImageFile() != null ? dto.getImageFile().getContentType() : "null");
        validateLanguage(dto.getLanguage());
        if (!StringUtils.hasText(dto.getGame())) {
            throw new RuntimeException("게임을 선택해주세요.");
        }
        String imageUrl = fileUploadService.uploadFile(dto.getImageFile(), "sealed");

        SealedProduct sealedProduct = new SealedProduct();
        sealedProduct.setProductNameEn(dto.getProductNameEn());
        sealedProduct.setProductNameKo(dto.getProductNameKo());
        sealedProduct.setGame(GameEnum.toConfigGame(dto.getGame()));
        sealedProduct.setSetName(dto.getSetName());
        sealedProduct.setSetCode(dto.getSetCode());
        sealedProduct.setPrice(dto.getPrice());
        sealedProduct.setCurrentVisibleStock(dto.getCurrentVisibleStock());
        sealedProduct.setTotalStock(dto.getTotalStock());
        sealedProduct.setMaxVisibleStock(dto.getMaxVisibleStock());
        sealedProduct.setImageUrl(imageUrl);
        sealedProduct.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        sealedProduct.setIsDeleted(false);
        sealedProduct.setLanguage(dto.getLanguage());
        sealedProduct.setOfflineProductId(normalizeOfflineProductId(dto.getOfflineProductId()));

        sealedProductRepository.save(sealedProduct);
        linkOfflineProduct(sealedProduct.getOfflineProductId(), "Sealed", sealedProduct.getId());
        applicationEventPublisher.publishEvent(new SealedProductChangeEvent(sealedProduct));
    }

    @Override
    public SealedProductDto getSealedProductById(Long id) {
        SealedProduct p = sealedProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SealedProduct not found: " + id));
        return toDto(p);
    }

    private void linkOfflineProduct(String offlineProductId, String tableName, Long linkId) {
        if (!StringUtils.hasText(offlineProductId)) return;
        offlineProductRepository.findByProductId(offlineProductId).ifPresent(op -> {
            op.setLinkTableName(tableName);
            op.setLinkId(linkId);
            offlineProductRepository.save(op);
        });
    }

    private void unlinkOfflineProduct(String tableName, Long linkId) {
        offlineProductRepository.findByLinkTableNameAndLinkId(tableName, linkId).ifPresent(op -> {
            op.setLinkTableName(null);
            op.setLinkId(null);
            offlineProductRepository.save(op);
        });
    }

    @Override
    public Page<SealedProductDto> getSealedProductList(String keyword, String game, String setCode, String language, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword : null;
        String gm = StringUtils.hasText(game) ? game : null;
        String sc = StringUtils.hasText(setCode) ? setCode : null;
        String lang = StringUtils.hasText(language) ? language : null;
        return sealedProductRepository.findByFilters(kw, gm, sc, lang, pageable).map(this::toDto);
    }

    @Override
    public List<SealedProductGameFacetDto> getFacets() {
        List<Object[]> rows = sealedProductRepository.findDistinctGameSetCodes();
        Map<String, List<SealedProductGameFacetDto.SetItemDto>> grouped = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String game = (String) row[0];
            String setCode = (String) row[1];
            String setName = (String) row[2];
            grouped.computeIfAbsent(game, k -> new ArrayList<>())
                   .add(new SealedProductGameFacetDto.SetItemDto(setCode, setName));
        }
        List<SealedProductGameFacetDto> result = new ArrayList<>();
        grouped.forEach((game, sets) -> result.add(new SealedProductGameFacetDto(game, sets)));
        return result;
    }

    @Override
    @Transactional
    public void updateSealedProduct(Long id, UpdateSealedProductDto dto) throws IOException {
        validateLanguage(dto.getLanguage());
        if (!StringUtils.hasText(dto.getGame())) {
            throw new RuntimeException("게임을 선택해주세요.");
        }
        SealedProduct sealedProduct = sealedProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SealedProduct not found: " + id));
        sealedProduct.setProductNameEn(dto.getProductNameEn());
        sealedProduct.setProductNameKo(dto.getProductNameKo());
        sealedProduct.setGame(GameEnum.toConfigGame(dto.getGame()));
        sealedProduct.setSetName(dto.getSetName());
        sealedProduct.setSetCode(dto.getSetCode());
        sealedProduct.setPrice(dto.getPrice());
        sealedProduct.setCurrentVisibleStock(dto.getCurrentVisibleStock());
        sealedProduct.setTotalStock(dto.getTotalStock());
        sealedProduct.setMaxVisibleStock(dto.getMaxVisibleStock());
        sealedProduct.setLanguage(dto.getLanguage());
        sealedProduct.setOfflineProductId(normalizeOfflineProductId(dto.getOfflineProductId()));
        if (dto.getImageFile() != null && !dto.getImageFile().isEmpty()) {
            sealedProduct.setImageUrl(fileUploadService.uploadFile(dto.getImageFile(), "sealed"));
        }
        if (dto.getIsActive() != null) {
            sealedProduct.setIsActive(dto.getIsActive());
        }
        sealedProductRepository.save(sealedProduct);
        applicationEventPublisher.publishEvent(new SealedProductChangeEvent(sealedProduct));
    }

    private SealedProductDto toDto(SealedProduct p) {
        return SealedProductDto.builder()
                .id(p.getId())
                .productNameEn(p.getProductNameEn())
                .productNameKo(p.getProductNameKo())
                .game(p.getGame())
                .setName(p.getSetName())
                .setCode(p.getSetCode())
                .price(p.getPrice())
                .currentVisibleStock(p.getCurrentVisibleStock())
                .totalStock(p.getTotalStock())
                .maxVisibleStock(p.getMaxVisibleStock())
                .imageUrl(p.getImageUrl())
                .isActive(p.getIsActive())
                .isDeleted(p.getIsDeleted())
                .publicId(p.getPublicId())
                .language(p.getLanguage())
                .offlineProductId(p.getOfflineProductId())
                .build();
    }

    private String normalizeOfflineProductId(String offlineProductId) {
        return StringUtils.hasText(offlineProductId) ? offlineProductId.trim() : null;
    }

    private void validateLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "언어 코드는 필수입니다.");
        }
        Set<String> activeCodes = cardProductLanguageRepository.findByIsActiveTrueOrderByCodeAsc()
                .stream()
                .map(l -> l.getCode().toLowerCase())
                .collect(Collectors.toSet());
        if (!activeCodes.contains(language.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 언어 코드입니다: " + language);
        }
    }

    @Override
    @Transactional
    public void deleteSealedProduct(Long id) {
        SealedProduct sealedProduct = sealedProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SealedProduct not found: " + id));
        sealedProduct.setIsDeleted(true);
        sealedProduct.setIsActive(false);
        sealedProductRepository.save(sealedProduct);
        unlinkOfflineProduct("Sealed", sealedProduct.getId());
        applicationEventPublisher.publishEvent(new SealedProductChangeEvent(sealedProduct));
    }

    @Override
    @Transactional
    public Long simpleRegisterFromOffline(OfflineProduct offlineProduct) {
        String title = offlineProduct.getTitle();
        Long price = offlineProduct.getPriceValue() != null
                ? offlineProduct.getPriceValue().longValue()
                : null;

        SealedProduct sealedProduct = new SealedProduct();
        sealedProduct.setProductNameEn(title);
        sealedProduct.setProductNameKo(title);
        sealedProduct.setPrice(price);
        sealedProduct.setCurrentVisibleStock(0);
        sealedProduct.setTotalStock(0);
        sealedProduct.setIsActive(false);
        sealedProduct.setIsDeleted(false);
        sealedProduct.setOfflineProductId(offlineProduct.getProductId());

        sealedProductRepository.save(sealedProduct);
        linkOfflineProduct(offlineProduct.getProductId(), "Sealed", sealedProduct.getId());
        applicationEventPublisher.publishEvent(new SealedProductChangeEvent(sealedProduct));
        return sealedProduct.getId();
    }
}
