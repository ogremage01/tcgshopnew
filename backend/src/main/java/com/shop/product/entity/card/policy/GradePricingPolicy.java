package com.shop.product.entity.card.policy;

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

import com.shop.product.dto.card.management.GradePricingPolicyDto;

import jakarta.persistence.Column;

@Entity
@Table(name = "grade_pricing_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradePricingPolicy {

    // 등급 가격 정책 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 등급(NM/EX/VG/G)
    @Column(name = "grade", nullable = false)
    private String grade;

    // 배율(0.00~)
    @Column(name = "percentage", nullable = false)
    private Double percentage;

    public GradePricingPolicyDto toDto() {
        return GradePricingPolicyDto.builder()
                .id(id)
                .grade(grade)
                .percentage(percentage)
                .build();
    }
}