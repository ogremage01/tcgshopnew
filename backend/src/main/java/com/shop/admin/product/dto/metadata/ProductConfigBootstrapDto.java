package com.shop.admin.product.dto.metadata;

import java.util.List;

import com.shop.log.sync.dto.LastSyncTimesDto;
import com.shop.product.dto.card.management.GradePricingPolicyDto;
import com.shop.product.dto.card.management.PriceConfigDto;
import com.shop.product.dto.card.management.TcgPSyncGameDto;
import com.shop.product.metadata.dto.StorageDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상품 설정 페이지 초기 로딩용 집계 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductConfigBootstrapDto {

    private SyncSection sync;
    private List<StorageDto> storage;
    private PriceSection price;

    public void setSync(SyncSection sync) {
        this.sync = sync;
    }

    public void setStorage(List<StorageDto> storage) {
        this.storage = storage;
    }

    public void setPrice(PriceSection price) {
        this.price = price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncSection {
        private List<TcgPSyncGameDto> games;
        private LastSyncTimesDto lastTime;

        public void setGames(List<TcgPSyncGameDto> games) {
            this.games = games;
        }

        public void setLastTime(LastSyncTimesDto lastTime) {
            this.lastTime = lastTime;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceSection {
        /** 게임별 최소 가격 목록 (configGame = GameEnum.game 풀네임) */
        private List<PriceConfigDto> minimumPrices;
        private List<GradePricingPolicyDto> grade;
        /** 게임별 환율 목록 (configGame = GameEnum.game 풀네임) */
        private List<PriceConfigDto> currencyRates;

        public void setMinimumPrices(List<PriceConfigDto> minimumPrices) {
            this.minimumPrices = minimumPrices;
        }

        public void setGrade(List<GradePricingPolicyDto> grade) {
            this.grade = grade;
        }

        public void setCurrencyRates(List<PriceConfigDto> currencyRates) {
            this.currencyRates = currencyRates;
        }
    }
}
