package com.shop.admin.product.controller.metadata;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shop.product.dto.card.management.GradePricingPolicyDto;
import com.shop.product.dto.card.management.PriceConfigDto;
import com.shop.product.enums.GameEnum;
import com.shop.product.service.pricing.GradePricingPolicyService;
import com.shop.product.service.pricing.PriceConfigService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/product/metadata")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMetadataPricingController {

    private final PriceConfigService priceConfigService;
    private final GradePricingPolicyService gradePricingPolicyService;

    // ── 최소 가격 ──────────────────────────────────────────────────

    @GetMapping("/minimum-price/{game}")
    public ResponseEntity<PriceConfigDto> getMinimumPrice(@PathVariable String game) {
        String configGame = resolveConfigGame(game);
        Optional<PriceConfigDto> opt = priceConfigService.getPriceConfig("minimum_price", configGame);
        return ResponseEntity.ok(opt.orElse(
                PriceConfigDto.builder()
                        .configKey("minimum_price")
                        .configGame(configGame)
                        .build()));
    }

    @PostMapping("/minimum-price/{game}")
    public ResponseEntity<String> setMinimumPrice(
            @PathVariable String game,
            @RequestBody PriceConfigDto priceConfigDto) {
        String configGame = resolveConfigGame(game);
        priceConfigService.setPriceConfig(
                PriceConfigDto.builder()
                        .configKey("minimum_price")
                        .configGame(configGame)
                        .configValue(priceConfigDto.getConfigValue())
                        .build());
        return ResponseEntity.ok("Minimum price config set successfully.");
    }

    // ── 환율 ───────────────────────────────────────────────────────

    @GetMapping("/us-currency-rate")
    public ResponseEntity<List<PriceConfigDto>> getAllCurrencyRates() {
        return ResponseEntity.ok(priceConfigService.getPriceConfigsByKey("us_currency_rate"));
    }

    @GetMapping("/us-currency-rate/{game}")
    public ResponseEntity<PriceConfigDto> getUsCurrencyRate(@PathVariable String game) {
        String configGame = resolveConfigGame(game);
        Optional<PriceConfigDto> opt = priceConfigService.getPriceConfig("us_currency_rate", configGame);
        return ResponseEntity.ok(opt.orElse(
                PriceConfigDto.builder()
                        .configKey("us_currency_rate")
                        .configGame(configGame)
                        .build()));
    }

    @PostMapping("/us-currency-rate/{game}")
    public ResponseEntity<String> setUsCurrencyRate(
            @PathVariable String game,
            @RequestBody PriceConfigDto priceConfigDto) {
        String configGame = resolveConfigGame(game);
        priceConfigService.setPriceConfig(
                PriceConfigDto.builder()
                        .configKey("us_currency_rate")
                        .configGame(configGame)
                        .configValue(priceConfigDto.getConfigValue())
                        .build());
        return ResponseEntity.ok("US currency rate config set successfully.");
    }

    // ── 등급별 가격 정책 ───────────────────────────────────────────

    @GetMapping("/grade-price")
    public ResponseEntity<List<GradePricingPolicyDto>> getGradePrice() {
        return ResponseEntity.ok(gradePricingPolicyService.getGradePricingPolicies());
    }

    @PostMapping("/grade-price")
    public ResponseEntity<String> setGradePrice(@RequestBody GradePricingPolicyDto gradePricingPolicyDto) {
        gradePricingPolicyService.setGradePricingPolicy(gradePricingPolicyDto);
        return ResponseEntity.ok("Grade pricing policy set successfully.");
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    /** gameAbbr ("mtg", "fab" ...) → GameEnum.game 풀네임 변환 */
    private String resolveConfigGame(String gameAbbr) {
        return GameEnum.fromCode(gameAbbr).getGame();
    }
}
