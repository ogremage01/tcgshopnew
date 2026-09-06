package com.shop.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.product.dto.ProductIpCreateRequestDto;
import com.shop.product.dto.ProductIpDto;
import com.shop.product.dto.manual.ProductCategoryCreateRequestDto;
import com.shop.product.dto.manual.ProductCategoryDto;

public interface ProductMasterService {

    void createCategory(ProductCategoryCreateRequestDto request);

    void updateCategory(Long id, String nameEn, String nameKo);

    void deleteCategory(Long id);

    List<ProductCategoryDto> getCategoryList();

    Page<ProductCategoryDto> getCategoryListByPage(Pageable pageable);

    void createProductIp(ProductIpCreateRequestDto request);

    void updateProductIp(Long id, String nameEn, String nameKo);

    void deleteProductIp(Long id);

    List<ProductIpDto> getProductIpList();

    Page<ProductIpDto> getProductIpListByPage(Pageable pageable);
}
