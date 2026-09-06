package com.shop.product.dto.card.slim;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPPriceCardSlimDto {

    // 카드 정보 간략 정보 Dto

    // 카드 ID
    private Long id;
    // 제품 고유 ID
    private Long productId;
    // 소속 게임
    private String game;
    // 상품 이름
    private String productName;
    // 상품 타입(Card/Sealed Product)
    private String type;
    // 레어도
    private String rarity;
    // 시장 가격
    private BigDecimal marketPrice;

    // 세트코드-넘버 형식(인덱스화 필요)
    // private Set<String> codeNumbers;

    // 양면카드 여부
    private Boolean isDoubleSided;

    // 세트약어
    private String setAbbrv;

    // 인쇄 형태(포일/노멀)
    private String printType;
    // 인쇄 방식((콜드,홀로...)포일/노멀)
    private String printing;

    // 카드 상태
    private String condition;

}
