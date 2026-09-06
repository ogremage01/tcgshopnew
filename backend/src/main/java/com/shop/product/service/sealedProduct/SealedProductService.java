package com.shop.product.service.sealedProduct;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.product.dto.sealed.AddSealedProductDto;
import com.shop.product.dto.sealed.SealedProductDto;
import com.shop.product.dto.sealed.UpdateSealedProductDto;
import com.shop.product.dto.sealed.SealedProductGameFacetDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;

public interface SealedProductService {

    void createSealedProduct(AddSealedProductDto dto) throws IOException;

    Page<SealedProductDto> getSealedProductList(String keyword, String game, String setCode, String language, Pageable pageable);

    List<SealedProductGameFacetDto> getFacets();

    SealedProductDto getSealedProductById(Long id);

    void updateSealedProduct(Long id, UpdateSealedProductDto dto) throws IOException;

    void deleteSealedProduct(Long id);

    Long simpleRegisterFromOffline(OfflineProduct offlineProduct);
}
