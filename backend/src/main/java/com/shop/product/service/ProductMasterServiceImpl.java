package com.shop.product.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.product.dto.ProductIpCreateRequestDto;
import com.shop.product.dto.ProductIpDto;
import com.shop.product.dto.manual.ProductCategoryCreateRequestDto;
import com.shop.product.dto.manual.ProductCategoryDto;
import com.shop.product.entity.ProductIp;
import com.shop.product.entity.manualProduct.ProductCategory;
import com.shop.product.repository.ProductIpRepository;
import com.shop.product.repository.manualProduct.ProductCategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductMasterServiceImpl implements ProductMasterService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductIpRepository productIpRepository;

    @Override
    @Transactional
    public void createCategory(ProductCategoryCreateRequestDto request) {
        if (productCategoryRepository.findByNameEn(request.getNameEn()).isPresent()) {
            throw new RuntimeException("카테고리 영문명이 이미 존재합니다.");
        }
        productCategoryRepository.save(ProductCategory.builder()
                .nameEn(request.getNameEn())
                .nameKo(request.getNameKo())
                .isDeleted(false)
                .build());
    }

    @Override
    @Transactional
    public void updateCategory(Long id, String nameEn, String nameKo) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        if (nameEn != null && !nameEn.equals(category.getNameEn())
                && productCategoryRepository.findByNameEn(nameEn).isPresent()) {
            throw new RuntimeException("카테고리 영문명이 이미 존재합니다.");
        }
        if (nameEn != null) {
            category.setNameEn(nameEn);
        }
        if (nameKo != null) {
            category.setNameKo(nameKo);
        }
        productCategoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다."));
        category.setIsDeleted(true);
        productCategoryRepository.save(category);
    }

    @Override
    public List<ProductCategoryDto> getCategoryList() {
        return productCategoryRepository.findAllByIsDeleted(false).stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductCategoryDto> getCategoryListByPage(Pageable pageable) {
        return productCategoryRepository.findAllByIsDeleted(false, pageable).map(this::toCategoryDto);
    }

    @Override
    @Transactional
    public void createProductIp(ProductIpCreateRequestDto request) {
        if (productIpRepository.findByNameEn(request.getNameEn()).isPresent()) {
            throw new RuntimeException("제품 IP 영문명이 이미 존재합니다.");
        }
        productIpRepository.save(ProductIp.builder()
                .nameEn(request.getNameEn())
                .nameKo(request.getNameKo())
                .isDeleted(false)
                .build());
    }

    @Override
    @Transactional
    public void updateProductIp(Long id, String nameEn, String nameKo) {
        ProductIp productIp = productIpRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("제품 IP를 찾을 수 없습니다."));
        if (nameEn != null && !nameEn.equals(productIp.getNameEn())
                && productIpRepository.findByNameEn(nameEn).isPresent()) {
            throw new RuntimeException("제품 IP 영문명이 이미 존재합니다.");
        }
        if (nameEn != null) {
            productIp.setNameEn(nameEn);
        }
        if (nameKo != null) {
            productIp.setNameKo(nameKo);
        }
        productIpRepository.save(productIp);
    }

    @Override
    @Transactional
    public void deleteProductIp(Long id) {
        ProductIp productIp = productIpRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("제품 IP를 찾을 수 없습니다."));
        productIp.setIsDeleted(true);
        productIpRepository.save(productIp);
    }

    @Override
    public List<ProductIpDto> getProductIpList() {
        return productIpRepository.findAllByIsDeleted(false).stream()
                .map(this::toProductIpDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductIpDto> getProductIpListByPage(Pageable pageable) {
        return productIpRepository.findAllByIsDeleted(false, pageable).map(this::toProductIpDto);
    }

    private ProductCategoryDto toCategoryDto(ProductCategory category) {
        return ProductCategoryDto.builder()
                .id(category.getId())
                .nameEn(category.getNameEn())
                .nameKo(category.getNameKo())
                .build();
    }

    private ProductIpDto toProductIpDto(ProductIp productIp) {
        return ProductIpDto.builder()
                .id(productIp.getId())
                .nameEn(productIp.getNameEn())
                .nameKo(productIp.getNameKo())
                .build();
    }
}
