package com.shop.admin.product.dto.card;

import com.shop.product.dto.card.management.CardProductRegisterCommand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductRegister {

    // 삭제 예정
    // 카드 등록 요청 정보 Dto

    // 제품 타입(Card/Sealed Product/supply?)
    private String productType;
    // 카드 이름
    private String cardName;
    // 카드 상태
    private String condition;
    // 인쇄 형태(포일/노멀)
    private String printType;
    // 언어
    private String language;
    // 표시 여부
    private Boolean isVisible;
    // 삭제 여부
    private Boolean isDeleted;
    // 표시 재고
    private Long currentVisibleStock;
    // 최대 표시 수량
    private Long maxVisibleStock;
    // 총 수량
    private Long totalStock;
    // 자동 수량 업데이트 여부
    private Boolean isAutoUpdatedStock;
    // 저장소 ID
    private Long storageId;
    // 가격 연동 사용여부
    private Boolean isPriceLinked;
    // (가격 연동 시) 카드 가격 배율
    private Double pricingRate;
    // (가격 연동 하지 않을 시) 설정 가격
    private Long price;
    // 메모
    private String memo;
    // UnionPrice ID
    private Long unionPriceId;

    public CardProductRegisterCommand toCommand() {
        return CardProductRegisterCommand.builder()
                .productType(productType)
                .cardName(cardName)
                .condition(condition)
                .printType(printType)
                .language(language)
                .isVisible(isVisible)
                .isDeleted(Boolean.TRUE.equals(isDeleted))
                .currentVisibleStock(currentVisibleStock)
                .maxVisibleStock(maxVisibleStock)
                .totalStock(totalStock)
                .isAutoUpdatedStock(isAutoUpdatedStock)
                .storageId(storageId)
                .isPriceLinked(isPriceLinked)
                .pricingRate(pricingRate)
                .price(price)
                .memo(memo)
                .unionPriceId(unionPriceId)
                .build();
    }
}