package com.shop.product.metadata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.shop.product.dto.card.management.TcgPSyncGameDto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tcg_p_sync_games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPSyncGame {

    // TCGPlayer 동기화 게임 엔티티(가격 동기화 시 게임 단위로 API 호출)
    // ex: MTG, FAB, etc.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 게임 라인 ID
    @Column(unique = true)
    private Long productLineId;
    // 게임 라인 이름
    private String productLineName;

    public TcgPSyncGameDto toDto() {
        return TcgPSyncGameDto.builder()
                .id(id)
                .productLineId(productLineId)
                .productLineName(productLineName)
                .build();
    }
}
