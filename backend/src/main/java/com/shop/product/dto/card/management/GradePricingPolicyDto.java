package com.shop.product.dto.card.management;

import com.shop.product.entity.card.policy.GradePricingPolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradePricingPolicyDto {

    // 등급 가격 정책 정보 Dto

    private Long id;
    // 등급(NM/EX/VG/G)
    private String grade;
    // 배율(0.00~1.00)
    private Double percentage;

    public GradePricingPolicy toEntity(Long id) {
        return GradePricingPolicy.builder()
                .id(id)
                .grade(grade)
                .percentage(percentage)
                .build();
    }
}
