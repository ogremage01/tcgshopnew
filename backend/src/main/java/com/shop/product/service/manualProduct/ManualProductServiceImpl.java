package com.shop.product.service.manualProduct;

import java.io.IOException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shop.common.fileUpload.service.FileUploadService;
import com.shop.common.util.UlidGenerator;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.product.dto.manual.AddManualProductDto;
import com.shop.product.dto.manual.ManualProductDto;
import com.shop.product.dto.manual.UpdateManualProductDto;
import com.shop.product.entity.ProductIp;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.manualProduct.ProductCategory;
import com.shop.product.repository.ProductIpRepository;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.manualProduct.ProductCategoryRepository;
import com.shop.search.event.ManualProductChangeEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManualProductServiceImpl implements ManualProductService {

    private final ManualProductRepository manualProductRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductIpRepository productIpRepository;
    private final FileUploadService fileUploadService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OfflineProductRepository offlineProductRepository;

    @Override
    @Transactional
    public void createManualProduct(AddManualProductDto addManualProductDto) throws IOException {
        ProductCategory category = requireCategoryByNameEn(addManualProductDto.getProductType());
        ProductIp productIp = requireProductIpByNameEn(addManualProductDto.getProductIp());

        String imageUrl = fileUploadService.uploadFile(addManualProductDto.getImageFile(), "manual_products");
        ManualProduct manualProduct = ManualProduct.builder()
                .nameEn(addManualProductDto.getNameEn())
                .nameKo(addManualProductDto.getNameKo())
                .description(addManualProductDto.getDescription())
                .price(addManualProductDto.getPrice())
                .stock(addManualProductDto.getStock())
                .productType(category.getNameEn())
                .productIp(productIp.getNameEn())
                .imgUrl(imageUrl)
                .isDeleted(false)
                .isVisible(addManualProductDto.getIsVisible())
                .publicId(UlidGenerator.nextUlid())
                .offlineProductId(normalizeOfflineProductId(addManualProductDto.getOfflineProductId()))
                .build();

        manualProductRepository.save(manualProduct);
        linkOfflineProduct(manualProduct.getOfflineProductId(), "Manual", manualProduct.getId());
        applicationEventPublisher.publishEvent(new ManualProductChangeEvent(manualProduct));
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
    @Transactional
    public void updateManualProduct(Long id, UpdateManualProductDto request) throws IOException {
        ManualProduct manualProduct = manualProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ManualProduct not found"));
        ProductCategory category = requireCategoryByNameEn(request.getProductType());
        ProductIp productIp = requireProductIpByNameEn(request.getProductIp());

        manualProduct.setNameEn(request.getNameEn());
        manualProduct.setNameKo(request.getNameKo());
        manualProduct.setDescription(request.getDescription());
        manualProduct.setPrice(request.getPrice());
        manualProduct.setStock(request.getStock());
        manualProduct.setProductType(category.getNameEn());
        manualProduct.setProductIp(productIp.getNameEn());
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            manualProduct.setImgUrl(fileUploadService.uploadFile(request.getImageFile(), "manual_products"));
        }
        manualProduct.setIsDeleted(false);
        if (request.getIsVisible() != null) {
            manualProduct.setIsVisible(request.getIsVisible());
        }
        manualProduct.setOfflineProductId(normalizeOfflineProductId(request.getOfflineProductId()));
        manualProductRepository.save(manualProduct);
        applicationEventPublisher.publishEvent(new ManualProductChangeEvent(manualProduct));
    }

    @Override
    public Page<ManualProductDto> getManualProductList(String keyword, Pageable pageable) {
        return manualProductRepository.findByKeywordForAdmin(keyword, pageable).map(this::toDto);
    }

    @Override
    public Page<ManualProductDto> getManualProductList(Pageable pageable) {
        return manualProductRepository.findAllForAdmin(pageable).map(this::toDto);
    }

    @Override
    public ManualProductDto getManualProductById(Long id) {
        ManualProduct manualProduct = manualProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ManualProduct not found"));
        return toDto(manualProduct);
    }

    private ManualProductDto toDto(ManualProduct manualProduct) {
        ProductCategory category = StringUtils.hasText(manualProduct.getProductType())
                ? productCategoryRepository.findByNameEn(manualProduct.getProductType()).orElse(null)
                : null;
        ProductIp productIp = StringUtils.hasText(manualProduct.getProductIp())
                ? productIpRepository.findByNameEn(manualProduct.getProductIp()).orElse(null)
                : null;

        return ManualProductDto.builder()
                .id(manualProduct.getId())
                .nameEn(manualProduct.getNameEn())
                .nameKo(manualProduct.getNameKo())
                .productType(manualProduct.getProductType())
                .categoryNameEn(category != null ? category.getNameEn() : manualProduct.getProductType())
                .categoryNameKo(category != null ? category.getNameKo() : null)
                .description(manualProduct.getDescription())
                .price(manualProduct.getPrice())
                .stock(manualProduct.getStock())
                .productIp(manualProduct.getProductIp())
                .productIpNameEn(productIp != null ? productIp.getNameEn() : manualProduct.getProductIp())
                .productIpNameKo(productIp != null ? productIp.getNameKo() : null)
                .imgUrl(manualProduct.getImgUrl())
                .isDeleted(manualProduct.getIsDeleted())
                .isVisible(manualProduct.getIsVisible())
                .publicId(manualProduct.getPublicId())
                .offlineProductId(manualProduct.getOfflineProductId())
                .build();
    }

    private ProductCategory requireCategoryByNameEn(String nameEn) {
        if (!StringUtils.hasText(nameEn)) {
            throw new RuntimeException("상품 카테고리를 선택해주세요.");
        }
        return productCategoryRepository.findByNameEn(nameEn.trim())
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
    }

    private ProductIp requireProductIpByNameEn(String nameEn) {
        if (!StringUtils.hasText(nameEn)) {
            throw new RuntimeException("제품 IP를 선택해주세요.");
        }
        return productIpRepository.findByNameEn(nameEn.trim())
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("제품 IP를 찾을 수 없습니다."));
    }

    private String normalizeOfflineProductId(String offlineProductId) {
        return StringUtils.hasText(offlineProductId) ? offlineProductId.trim() : null;
    }

    @Override
    @Transactional
    public void deleteManualProduct(Long id) {
        ManualProduct manualProduct = manualProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ManualProduct not found"));
        manualProduct.setIsDeleted(true);
        manualProduct.setIsVisible(false);
        manualProductRepository.save(manualProduct);
        unlinkOfflineProduct("Manual", manualProduct.getId());
        applicationEventPublisher.publishEvent(new ManualProductChangeEvent(manualProduct));
    }

    @Override
    @Transactional
    public Long simpleRegisterFromOffline(OfflineProduct offlineProduct) {
        String title = offlineProduct.getTitle();
        Long price = offlineProduct.getPriceValue() != null
                ? offlineProduct.getPriceValue().longValue()
                : null;

        ManualProduct manualProduct = ManualProduct.builder()
                .nameEn(title)
                .nameKo(title)
                .price(price)
                .stock(0L)
                .isDeleted(false)
                .isVisible(false)
                .offlineProductId(offlineProduct.getProductId())
                .build();

        manualProductRepository.save(manualProduct);
        linkOfflineProduct(offlineProduct.getProductId(), "Manual", manualProduct.getId());
        applicationEventPublisher.publishEvent(new ManualProductChangeEvent(manualProduct));
        return manualProduct.getId();
    }
}
