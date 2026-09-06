package com.shop.admin.product.service.metadata;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.admin.product.dto.metadata.ProductConfigBootstrapDto;
import com.shop.log.sync.dto.LastSyncTimesDto;
import com.shop.log.sync.service.SyncLogService;
import com.shop.product.dto.card.management.GradePricingPolicyDto;
import com.shop.product.dto.card.management.PriceConfigDto;
import com.shop.product.dto.card.management.TcgPSyncGameDto;
import com.shop.product.metadata.dto.StorageDto;
import com.shop.product.service.pricing.GradePricingPolicyService;
import com.shop.product.service.pricing.PriceConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminProductConfigBootstrapServiceImpl implements AdminProductConfigBootstrapService {

    private final AdminProductMetadataService adminProductMetadataService;
    private final SyncLogService syncLogService;
    private final PriceConfigService priceConfigService;
    private final GradePricingPolicyService gradePricingPolicyService;

    @Override
    @Transactional(readOnly = true)
    public ProductConfigBootstrapDto getBootstrap() {
        List<TcgPSyncGameDto> games = adminProductMetadataService.getSyncGameList();
        LastSyncTimesDto lastTime = syncLogService.getLastSyncTimes();
        List<StorageDto> storage = adminProductMetadataService.getStorageList();

        List<PriceConfigDto> minimumPrices = priceConfigService.getPriceConfigsByKey("minimum_price");
        List<PriceConfigDto> currencyRates = priceConfigService.getPriceConfigsByKey("us_currency_rate");
        List<GradePricingPolicyDto> grade = gradePricingPolicyService.getGradePricingPolicies();

        ProductConfigBootstrapDto.SyncSection syncSection = new ProductConfigBootstrapDto.SyncSection();
        syncSection.setGames(games);
        syncSection.setLastTime(lastTime);

        ProductConfigBootstrapDto.PriceSection priceSection = new ProductConfigBootstrapDto.PriceSection();
        priceSection.setMinimumPrices(minimumPrices);
        priceSection.setGrade(grade);
        priceSection.setCurrencyRates(currencyRates);

        ProductConfigBootstrapDto dto = new ProductConfigBootstrapDto();
        dto.setSync(syncSection);
        dto.setStorage(storage);
        dto.setPrice(priceSection);
        return dto;
    }
}
