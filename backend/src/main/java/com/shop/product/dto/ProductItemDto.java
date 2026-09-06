package com.shop.product.dto;

import java.util.List;
import java.util.Map;

import com.shop.product.dto.card.CardProductSaleDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductItemDto {

    // ---------공통 정보--------------------------------

    // 상품 종류
    private String productType;

    // 상품 이름
    private String productNameEn;
    // 상품 이름(한글)
    private String productNameKo;

    // 상품 이미지
    private String imageUrlEn;
    // 상품 이미지 (한글)
    private String imageUrlKo;
    private Map<String, String> languageImageUrlMap;

    // 양면카드 뒷면 이미지(영문)
    private String backImageUrlEn;
    // 양면카드 뒷면 이미지 (한글)
    private String backImageUrlKo;
    private Map<String, String> languageBackImageUrlMap;
    // 양면 카드인지 여부
    private Boolean isDoubleSided;

    // 상품 가격 (정렬할 때도 써야함)
    private Long price;

    // 표시 재고
    private Long currentVisibleStock;

    // 세트 정보-영어 기준
    private String setName;
    // 세트 코드
    private String setCode;

    // 테이블 종류 (manual/sealed 등)
    private String table;

    // 테이블 내 상품 ID (manual/sealed 등)
    private Long tableId;

    // 상품 IP (manual/sealed 등)
    private String productIp;

    // ------카드 상품일 때--------------------------------
    private CardProductInfoDto card;

    // ------supplies 상품일 때--------------------------------
    private SupplyProductInfoDto supplyProductInfoDto;

    // 적립율 (매칭된 규칙의 rewardPercentage)
    private Double rewardPercentage;
    // 적립액 (price * rewardPercentage, 내림)
    private Long saveAmount;
    // 매칭된 적립 규칙 ID
    private Long matchedRuleId;

    // ---------
    // 밀봉 제품 판매 정보
    private SealedProductInfoDto sealedProductInfoDto;

    // ---------manual 상품일 때--------------------------------
    private ManualProductInfoDto manualProductInfoDto;

    // ---------카드 상품일 때--------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardProductInfoDto {
        // 게임(MTG/FAB 외 게임 라인)
        private String game;
        // 카드 상품 판매 정보
        private Map<String, List<CardProductSaleDto>> cardProductSaleDtoMap;
        // 레어도
        private String rarity;
        // 인쇄 형태(포일/노멀)
        private String printType;
        // 인쇄 방식(Normal/Foil/Rainbow Foil....)
        private String printing;
        // 카드 세트 코드
        private String setCode;
        // 카드 세트 번호
        private String setNumber;
        // 카드 세트 이름
        private String setName;
        // UnionPrice ID
        private Long unionPriceId;
        // UnionPrice 공용 ID (상세 URL용)
        private String publicId;
        // 판매 정보
        private GameSalesInfoDto gameSalesInfo;
    }

    // ---------밀봉 제품일 때--------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SealedProductInfoDto {
        // 밀봉 제품 게임
        private String game;
        // 밀봉 제품 공용 ID
        private String publicId;
        // 밀봉 제품 판매 정보
        private Map<String, List<SealedProductSaleDto>> sealedProductSaleDtoMap;
        // UnionPrice ID
        // 판매 정보
        private GameSalesInfoDto gameSalesInfo;
    }

    // ---------manual 상품일 때--------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManualProductInfoDto {
        // manual 상품 종류
        private String productType;
        // 검색 매핑 ID
        private Long searchMapId;
        // manual 상품 공용 ID
        private String publicId;
        // 상품 설명
        private String description;
    }

    // ---------supplies 상품일 때--------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplyProductInfoDto {
        // 검색 매핑 ID
        private Long searchMapId;
        // 서플라이 공용 ID
        private String publicId;
        // 서플라이 타입 (검색·필터용 저장값, nameKo)
        private String suppliesType;
        // 서플라이 타입 표시명
        private String suppliesTypeNameEn;
        private String suppliesTypeNameKo;
        // 테이블 종류
        private String table;
        // 테이블 내 상품 ID
        private Long tableId;
        // 제조사
        private String maker;
        // 상품 IP(MTG/FAB/해리포터 등등)
        private String productIp;
        // 상품 설명
        private String description;
    }
}
