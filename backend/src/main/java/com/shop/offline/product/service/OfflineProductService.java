package com.shop.offline.product.service;



import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;



import com.shop.offline.product.dto.OfflineProductDto;

import com.shop.offline.product.dto.OfflineProductReceivingHistoryDto;

import com.shop.offline.product.dto.OfflineProductReceivingRequest;

import com.shop.offline.product.dto.OfflineShippingReconcileResultDto;

import com.shop.offline.product.dto.OfflineStockSyncResultDto;

import com.shop.offline.product.dto.SimpleRegisterOfflineProductDto;

import com.shop.common.excel.dto.ExcelFileResult;



public interface OfflineProductService {



    void downloadOfflineProducts();

    void downloadOfflineProductCategories();

    ExcelFileResult downloadOfflineProductsExcel();

    Page<OfflineProductDto> getOfflineProductList(
            String sortBy,
            String sortDirection,
            Pageable pageable);

    Page<OfflineProductDto> getOfflineProductListByKeyword(
            String keyword,
            String sortBy,
            String sortDirection,
            Pageable pageable);

    OfflineProductDto getOfflineProductByProductId(String productId);



    void simpleRegisterOfflineProduct(SimpleRegisterOfflineProductDto simpleRegisterOfflineProductDto);



    OfflineProductReceivingHistoryDto registerReceiving(OfflineProductReceivingRequest request);



    Page<OfflineProductReceivingHistoryDto> getReceivingHistories(Pageable pageable);



    OfflineProductReceivingHistoryDto getReceivingHistory(Long id);

    /**
     * COMPLETED 매출 기준으로 shippingQuantity 전량 절대값 재집계 (idempotent).
     */
    OfflineShippingReconcileResultDto reconcileShippingQuantities();

    /**
     * 현재 저장된 입고/출고만으로 묶음 전환 후 온라인 재고를 동기화한다.
     * 출고 절대값 재집계는 하지 않는다.
     */
    OfflineStockSyncResultDto syncStockFromCurrentQuantities();

}
