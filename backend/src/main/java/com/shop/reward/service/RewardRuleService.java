package com.shop.reward.service;

import java.util.List;

import com.shop.reward.dto.RewardRuleDto;
import com.shop.reward.entity.RewardRule;

public interface RewardRuleService {
    RewardRule createRewardRule(RewardRuleDto rewardRuleDto);
    RewardRule updateRewardRule(Long id, RewardRuleDto rewardRuleDto);
    void deleteRewardRule(Long id);
    List<RewardRuleDto> getRewardRules();
    RewardRule getRewardRuleById(Long id);
    void reorderRules(List<Long> orderedIds);
}
