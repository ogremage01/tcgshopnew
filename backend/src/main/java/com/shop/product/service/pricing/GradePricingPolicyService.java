package com.shop.product.service.pricing;

import java.util.List;

import com.shop.product.dto.card.management.GradePricingPolicyDto;

public interface GradePricingPolicyService {

    // 등급 가격 정책 서비스

    List<GradePricingPolicyDto> getGradePricingPolicies();

    void setGradePricingPolicy(GradePricingPolicyDto gradePricingPolicyDto);

}
