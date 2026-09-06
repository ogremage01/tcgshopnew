package com.shop.reward.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RewardRuleMatchResult {
    private Long ruleId;
    private Double rewardPercentage;
    private Long saveAmount;
}
