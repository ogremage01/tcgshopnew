package com.shop.card.service;

import java.util.List;

import com.shop.product.dto.card.full.TcgPPriceDto;
import com.shop.product.dto.card.slim.TcgPPriceCardSlimDto;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TcgPPriceService {

    // TCGPlayer 가격 서비스

    /**
     * 상품 이름으로 검색하여 카드 정보 조회
     * 
     * @param productName 상품 이름
     * @return 카드 정보
     */
    public List<TcgPPriceCardSlimDto> findByProductName(String productName);

    /**
     * ID로 카드 정보 조회
     * 
     * @param id 카드 ID
     * @return 카드 정보
     */
    public Optional<TcgPPriceCardSlimDto> findById(Long id);

    /**
     * 상품 이름혹은 코드넘버로 검색하여 카드 정보 조회
     * 
     * @param keyword  검색 키워드
     * @param pageable 페이지 정보
     * @return 카드 정보
     */
    public Page<TcgPPriceCardSlimDto> findByProductNameOrCodeNumber(String keyword, Pageable pageable);

    /**
     * 게임과 세트명으로 카드 조회
     * 
     * @param game 게임
     * @param set  세트명
     * @return 카드 정보
     */
    public List<TcgPPriceDto> findByGameAndSetNameForExcelExport(String game, String set);

    /**
     * 상품 이름혹은 코드넘버로 검색하여 카드 정보 조회
     * 
     * @param keyword 검색 키워드
     * @return 카드 정보
     */
    public List<TcgPPriceCardSlimDto> findByProductNameOrCodeNumberList(String keyword);
}
