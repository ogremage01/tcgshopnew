package com.shop.admin.product.dto.card;

import com.querydsl.core.annotations.QueryProjection;
import com.shop.card.entity.UnionPrice;
import com.shop.product.dto.card.management.CardProductManagementDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@QueryProjection
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardProductManagementResponseDto {

    // 카드 관리 응답 정보 Dto
    // 카드 ID
    private Long id;
    // 카드 이름
    private String name;
    // 카드 이미지
    private String imageUrl;
    // 카드 정보
    private UnionPrice unionPrice;

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

    public static CardProductManagementResponseDto from(CardProductManagementDto dto) {
        return CardProductManagementResponseDto.builder()
                .id(dto.getId())
                .name(dto.getName())
                .imageUrl(dto.getImageUrl())
                .unionPrice(dto.getUnionPrice())
                .condition(dto.getCondition())
                .printType(dto.getPrintType())
                .language(dto.getLanguage())
                .isVisible(dto.getIsVisible())
                .isDeleted(dto.getIsDeleted())
                .currentVisibleStock(dto.getCurrentVisibleStock())
                .maxVisibleStock(dto.getMaxVisibleStock())
                .totalStock(dto.getTotalStock())
                .isAutoUpdatedStock(dto.getIsAutoUpdatedStock())
                .storageId(dto.getStorageId())
                .isPriceLinked(dto.getIsPriceLinked())
                .pricingRate(dto.getPricingRate())
                .price(dto.getPrice())
                .memo(dto.getMemo())
                .build();
    }
}
