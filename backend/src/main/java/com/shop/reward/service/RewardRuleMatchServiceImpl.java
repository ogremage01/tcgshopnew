package com.shop.reward.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.reward.dto.ProductMatchContext;
import com.shop.reward.dto.RewardRuleMatchResult;
import com.shop.reward.entity.RewardRule;
import com.shop.reward.repository.RewardRuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardRuleMatchServiceImpl implements RewardRuleMatchService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RewardRuleRepository rewardRuleRepository;

    @Override
    public Optional<RewardRuleMatchResult> match(ProductMatchContext context, long price) {
        LocalDateTime now = LocalDateTime.now();
        return rewardRuleRepository.findActiveOrderedByRank(now).stream()
                .filter(rule -> matches(rule, context))
                .findFirst()
                .map(rule -> {
                    double rate = rule.getRewardPercentage() != null ? rule.getRewardPercentage() : 0.0;
                    long saveAmount = (long) Math.floor(price * rate);
                    return RewardRuleMatchResult.builder()
                            .ruleId(rule.getId())
                            .rewardPercentage(rate)
                            .saveAmount(saveAmount)
                            .build();
                });
    }

    private boolean matches(RewardRule rule, ProductMatchContext context) {
        String calRule = rule.getCalRule();
        if (calRule == null || calRule.isBlank()) {
            return true;
        }
        try {
            Map<String, String> conditions = OBJECT_MAPPER.readValue(calRule, new TypeReference<>() {});
            return conditions.entrySet().stream().allMatch(entry -> {
                String contextValue = resolveContextField(context, entry.getKey());
                return Objects.equals(contextValue, entry.getValue());
            });
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException e) {
            log.warn("RewardRule[{}] calRule 파싱 실패: {}", rule.getId(), e.getMessage());
            return false;
        }
    }

    private String resolveContextField(ProductMatchContext context, String key) {
        return switch (key) {
            case "game"        -> context.getGame();
            case "productType" -> context.getProductType();
            case "condition"   -> context.getCondition();
            case "language"    -> context.getLanguage();
            case "set"         -> context.getSet();
            case "rarity"      -> context.getRarity();
            case "printing"    -> context.getPrinting();
            case "cardName"    -> context.getCardName();
            case "setNumber"   -> context.getSetNumber();
            default            -> null;
        };
    }
}
