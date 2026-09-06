package com.shop.product.dto.card.management;

import com.shop.product.metadata.entity.TcgPSyncGame;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPSyncGameDto {

    // TCG 동기화 게임 정보 Dto

    private Long id;
    // 게임 라인 ID
    private Long productLineId;
    // 게임 라인 이름
    private String productLineName;

    /**
     * TCG 동기화 게임 정보 Dto를 TCG 동기화 게임 엔티티로 변환
     *
     * @return TCG 동기화 게임 엔티티
     */
    public TcgPSyncGame toEntity() {
        return TcgPSyncGame.builder()
                .id(id)
                .productLineId(productLineId)
                .productLineName(productLineName)
                .build();
    }
}
