package com.shop.admin.product.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.reward.dto.RewardRuleDto;
import com.shop.reward.entity.RewardRule;
import com.shop.reward.service.RewardRuleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reward-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRewardRuleController {

    private final RewardRuleService rewardRuleService;

    @GetMapping
    public ResponseEntity<List<RewardRuleDto>> getRewardRules() {
        return ResponseEntity.ok(rewardRuleService.getRewardRules());
    }

    @PostMapping
    public ResponseEntity<RewardRule> createRewardRule(@RequestBody RewardRuleDto rewardRuleDto) {
        return ResponseEntity.ok(rewardRuleService.createRewardRule(rewardRuleDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RewardRule> updateRewardRule(@PathVariable Long id, @RequestBody RewardRuleDto rewardRuleDto) {
        return ResponseEntity.ok(rewardRuleService.updateRewardRule(id, rewardRuleDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRewardRule(@PathVariable Long id) {
        rewardRuleService.deleteRewardRule(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderRules(@RequestBody List<Long> orderedIds) {
        rewardRuleService.reorderRules(orderedIds);
        return ResponseEntity.ok().build();
    }
}
