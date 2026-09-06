package com.shop.reward.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

import com.shop.reward.entity.RewardRule;
import com.shop.reward.dto.RewardRuleDto;

import com.shop.reward.repository.RewardRuleRepository;

@Service
@RequiredArgsConstructor
public class RewardRuleServiceImpl implements RewardRuleService {

    private final RewardRuleRepository rewardRuleRepository;

    @Override
    @Transactional
    public RewardRule createRewardRule(RewardRuleDto rewardRuleDto) {
        Integer maxRank = rewardRuleRepository.findMaxRank();
        int nextRank = (maxRank == null ? 0 : maxRank) + 1;
        return rewardRuleRepository.save(RewardRule.builder()
            .name(rewardRuleDto.getName())
            .rank(nextRank)
            .rewardPercentage(rewardRuleDto.getRewardPercentage())
            .calRule(rewardRuleDto.getCalRule())
            .isActive(Boolean.TRUE.equals(rewardRuleDto.getIsActive()))
            .startAt(rewardRuleDto.getStartAt())
            .endAt(rewardRuleDto.getEndAt())
            .build());
    }

    @Override
    @Transactional
    public RewardRule updateRewardRule(Long id, RewardRuleDto rewardRuleDto) {
        RewardRule rule = rewardRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("RewardRule not found: " + id));
        rule.setName(rewardRuleDto.getName());
        rule.setRewardPercentage(rewardRuleDto.getRewardPercentage());
        rule.setCalRule(rewardRuleDto.getCalRule());
        rule.setActive(Boolean.TRUE.equals(rewardRuleDto.getIsActive()));
        rule.setStartAt(rewardRuleDto.getStartAt());
        rule.setEndAt(rewardRuleDto.getEndAt());
        return rewardRuleRepository.save(rule);
    }

    @Override
    public void deleteRewardRule(Long id) {
        rewardRuleRepository.deleteById(id);
    }

    @Override
    public List<RewardRuleDto> getRewardRules() {
        return rewardRuleRepository.findAllByOrderByRankAsc().stream()
            .map(rewardRule -> RewardRuleDto.builder()
                .id(rewardRule.getId())
                .name(rewardRule.getName())
                .rank(rewardRule.getRank())
                .rewardPercentage(rewardRule.getRewardPercentage())
                .calRule(rewardRule.getCalRule())
                .isActive(rewardRule.isActive())
                .startAt(rewardRule.getStartAt())
                .endAt(rewardRule.getEndAt())
                .createdAt(rewardRule.getCreatedAt())
                .updatedAt(rewardRule.getUpdatedAt())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    public RewardRule getRewardRuleById(Long id) {
        return rewardRuleRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void reorderRules(List<Long> orderedIds) {
        // 1단계: unique 제약 충돌 방지를 위해 음수 임시 rank 부여
        for (int i = 0; i < orderedIds.size(); i++) {
            rewardRuleRepository.updateRank(orderedIds.get(i), -(i + 1));
        }
        // 2단계: 최종 rank 1, 2, 3... 부여
        for (int i = 0; i < orderedIds.size(); i++) {
            rewardRuleRepository.updateRank(orderedIds.get(i), i + 1);
        }
    }
}
