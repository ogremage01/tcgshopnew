package com.shop.product.dto.supplies;

import com.shop.product.entity.supplies.Maker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakerDto {

    // 제조사 정보 Dto

    private Long id;
    // 제조사 이름
    private String name;

    /**
     * 제조사 정보 Dto를 제조사 엔티티로 변환
     * 
     * @return 제조사 엔티티
     */
    public Maker toEntity() {
        return Maker.builder()
                .id(id)
                .name(name)
                .build();
    }
}
