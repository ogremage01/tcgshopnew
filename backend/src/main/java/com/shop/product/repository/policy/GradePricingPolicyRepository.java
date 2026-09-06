package com.shop.product.repository.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.product.entity.card.policy.GradePricingPolicy;

import java.util.List;
import java.util.Optional;

public interface GradePricingPolicyRepository extends JpaRepository<GradePricingPolicy, Long> {

    // 등급 가격 정책 리포지토리

    List<GradePricingPolicy> findAllByGrade(String grade);

    Optional<GradePricingPolicy> findByGrade(String grade);

}
