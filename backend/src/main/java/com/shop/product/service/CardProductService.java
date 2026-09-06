package com.shop.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.admin.product.dto.card.CardProductAdminSearchResponseDto;
import com.shop.card.dto.slim.UnionPriceGameSetFacetDto;
import com.shop.product.dto.card.management.CardProductManagementDto;
import com.shop.product.dto.card.management.CardProductPatchCommand;
import com.shop.product.dto.card.management.CardProductRegisterCommand;
import com.shop.product.dto.card.management.SearchBySetCriteriaDto;

import java.util.List;
import java.io.IOException;
import com.shop.product.entity.card.CardProduct;

public interface CardProductService {

    // 카드 제품 서비스
    /**
     * 제품 ID와 삭제 여부로 카드 제품 목록을 조회한다.
     * 
     * @param productId 제품 ID
     * @param isDeleted 삭제 여부
     * @param pageable  페이지 정보
     * @return 카드 제품 목록
     */
    public Page<CardProductManagementDto> findByProductIdAndIsDeleted(Long productId, Boolean isDeleted,
            Pageable pageable);

    /**
     * 카드 제품을 등록한다.
     * 
     * 
     * @param cardProductRegisterDto 카드 제품 등록 정보
     */
    public void registerCardProduct(CardProductRegisterCommand cardProductRegisterDto);

    /**
     * 카드 제품을 수정한다.
     * 
     * @param id   카드 제품 ID
     * @param body 수정 정보
     * @return 카드 제품 정보
     */
    public CardProductManagementDto patchCardProduct(Long id, CardProductPatchCommand body);

    /**
     * 상품 이름혹은 코드넘버로 검색하여 카드 제품 목록을 조회한다.
     * 
     * @param keyword  검색 키워드
     * @param pageable 페이지 정보
     * @return 카드 제품 목록
     */
    public Page<CardProductManagementDto> findByProductNameOrCodeNumber(
            String keyword,
            String game,
            String setCode,
            Pageable pageable);

    public List<UnionPriceGameSetFacetDto> findGameSetFacetsByKeywordForAdmin(String keyword);

    public CardProductAdminSearchResponseDto searchByKeywordForAdmin(
            String keyword,
            String game,
            String setCode,
            Pageable pageable);

    /**
     * 카드 제품 ID 리스트로 카드 제품 목록을 조회한다.
     * 
     * @param cardIdList 카드 제품 ID 리스트
     * @param pageable   페이지 정보
     * @return 카드 제품 목록
     */
    public Page<CardProductManagementDto> findByProductIdInForAdmin(List<Long> cardIdList, Pageable pageable);

    /**
     * 카드 제품 목록을 등록 또는 수정한다.
     * 
     * @param cardProductList 카드 제품 목록
     */
    public int registerOrUpdateCardProducts(List<CardProduct> cardProductList) throws IOException;

    /**
     * 보관소 ID로 카드 제품을 삭제한다.
     * 
     * @param storageId 보관소 ID
     */
    public void deleteCardProductsByStorageId(Long storageId);

    /**
     * 카드 제품 ID로 카드 제품을 삭제한다.
     * 
     * @param id 카드 제품 ID
     */
    public void deleteCardProductById(Long id);

    /**
     * 세트별 싱글카드 현황을 조회한다.
     * 
     * @param searchBySetDto 세트별 싱글카드 현황 조회 정보
     * @return 세트별 싱글카드 현황
     */
    public Page<CardProductManagementDto> searchBySetForAdmin(SearchBySetCriteriaDto criteria, Pageable pageable);

}
