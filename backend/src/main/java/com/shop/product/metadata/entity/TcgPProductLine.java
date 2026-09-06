package com.shop.product.metadata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;

import com.shop.product.metadata.dto.TcgPProductLineDto;

@Entity
@Table(name = "product_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPProductLine {

    // TCGPlayer 게임 라인 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 게임 라인 ID
    @Column(unique = true)
    private Long productLineId;
    // 게임 라인 이름
    private String productLineName;

    // 게임 라인 URL 경로명(본 서비스에서는 현재 사용하고 있지 않고 있음)
    private String productLineUrlName;
    // 미사용
    private Boolean isDirect;

    public TcgPProductLineDto toDto() {
        return TcgPProductLineDto.builder()
                .productLineId(productLineId)
                .productLineName(productLineName)
                .build();
    }
}
