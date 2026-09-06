package com.shop.search.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.card.entity.UnionPrice;
import com.shop.product.dto.ProductItemDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.search.dto.searching.ProductSearchingDto;
import com.shop.search.dto.searching.SearchInitResponseDto;

public interface ProductSearchMapService {

    void syncProductSearchMap(CardProduct cardProduct);
    void syncProductSearchMap(SealedProduct sealedProduct);
    void syncProductSearchMap(ManualProduct manualProduct);
    void syncProductSearchMap(UnionPrice unionPrice);
    void syncProductSearchMap(Supply supply);
    void syncProductSearchMapBulk(List<CardProduct> cardProducts);
    void syncProductSearchMapBulkByUnionPrice(List<UnionPrice> unionPrices);

    void isvisibleChangeProductSearchMapByStorage(Long storageId);

    /** UNION_PRICE 대표 행 in_stock을 CardProduct 재고 집계와 일괄 정합 */
    void reconcileUnionPriceInStock();

    SearchInitResponseDto searchInit(ProductSearchingDto query, Pageable pageable);

    Page<ProductItemDto> searchProducts(ProductSearchingDto query, Pageable pageable);

    Page<ProductItemDto> searchProductsByGameSetCode(
            String game,
            String setCode,
            ProductSearchingDto query,
            Pageable pageable);

    ProductItemDto getProductDetail(String id);

    ProductItemDto getProductItemDtoBySearchMapId(Long searchMapId);

}
