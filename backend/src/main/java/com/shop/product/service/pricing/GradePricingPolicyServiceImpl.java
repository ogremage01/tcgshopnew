package com.shop.product.service.pricing;

import org.springframework.stereotype.Service;

import com.shop.product.dto.card.management.GradePricingPolicyDto;
import com.shop.product.entity.card.policy.GradePricingPolicy;
import com.shop.product.repository.policy.GradePricingPolicyRepository;
import com.shop.scheduler.price.application.selling.CardSellingPriceUpdateService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradePricingPolicyServiceImpl implements GradePricingPolicyService {

    private final GradePricingPolicyRepository gradePricingPolicyRepository;
    private final CardSellingPriceUpdateService cardSellingPriceUpdateService;

    @Override
    public List<GradePricingPolicyDto> getGradePricingPolicies() {
        return gradePricingPolicyRepository.findAll().stream()
                .map(GradePricingPolicy::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setGradePricingPolicy(GradePricingPolicyDto gradePricingPolicyDto) {
        GradePricingPolicy gradePricingPolicy = GradePricingPolicy.builder()
                .id(gradePricingPolicyDto.getId())
                .grade(gradePricingPolicyDto.getGrade())
                .percentage(gradePricingPolicyDto.getPercentage()).build();
        gradePricingPolicyRepository.save(gradePricingPolicy);
        cardSellingPriceUpdateService.startUpdateCardCalculatedLinkedPriceForGrade(gradePricingPolicyDto.getGrade());
    }

}
