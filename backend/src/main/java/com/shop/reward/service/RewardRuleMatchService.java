package com.shop.reward.service;

import java.util.Optional;

import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.dto.RewardRuleMatchResult;

public interface RewardRuleMatchService {

    Optional<RewardRuleMatchResult> match(ProductMatchContext context, long price);
}
