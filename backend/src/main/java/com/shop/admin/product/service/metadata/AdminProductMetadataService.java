package com.shop.admin.product.service.metadata;

import java.util.List;

import com.shop.product.dto.card.management.TcgPSyncGameDto;
import com.shop.product.metadata.dto.StorageDto;
import com.shop.product.metadata.dto.TcgPProductLineDto;
import com.shop.product.metadata.entity.TcgPSetName;

public interface AdminProductMetadataService {
    /**
     * 상품 라인 목록을 조회합니다.
     *
     * @return 상품 라인 목록
     */
    public List<TcgPProductLineDto> getProductLineList();

    /**
     * 상품 라인 기준 세트명을 조회합니다.
     *
     * @param productLineId 상품 라인 ID
     * @return 세트명 목록
     */

    public List<TcgPSetName> getSetNameList(Long productLineId);

    /**
     * 동기화 대상 게임 목록을 조회합니다.
     *
     * @return 동기화 대상 게임 목록
     */
    public List<TcgPSyncGameDto> getSyncGameList();

    /**
     * 보관소 목록을 조회합니다.
     *
     * @return 보관소 목록
     */
    public List<StorageDto> getStorageList();

    /**
     * 보관소를 등록합니다.
     *
     * @param storageDto 보관소 등록 정보
     */
    public void setStorage(StorageDto storageDto);

    /**
     * 보관소를 수정합니다.
     *
     * @param storageDto 보관소 수정 정보
     */
    public void updateStorage(StorageDto storageDto);

    /**
     * 보관소를 삭제합니다.
     *
     * @param storageDto 보관소 삭제 정보
     */
    public void deleteStorage(StorageDto storageDto);
}
