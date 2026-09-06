package com.shop.product.dto.card.full;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPPriceDto {
    // TCGPrice 정보 Dto

    private Long id;

    private Long productId;

    private String condition;
    // 게임 라인 ID
    private String game;
    // 미사용
    private Boolean isSupplemental;
    // 시장 가격
    private BigDecimal marketPrice;
    // 넘버
    private String number;
    // 프린트 타입
    private String printing;
    // 상품 이름
    private String productName;
    // 레어도
    private String rarity;
    // 세트 이름

    private String set;
    // 세트 약어
    private String setAbbrv;
    // 상품 타입(Card/Sealed Product)
    private String type;

    // 프린트 타입(포일/노멀)
    private String printType;
    // 양면카드 여부
    private Boolean isDoubleSided;
    // 이미지 다운로드 여부(false: 다운로드 안됨, true: 다운로드 됨)
    private Boolean downloaded;
    // 코드 넘버(세트-넘버)
    private String codeNumber;
    // 체크 코드
    private String checkCode;
}
