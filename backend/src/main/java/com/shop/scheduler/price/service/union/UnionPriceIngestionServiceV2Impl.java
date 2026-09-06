package com.shop.scheduler.price.service.union;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. union_prices 적재·검색맵 재구축 규칙은 포함하지 않습니다.
 */
@Primary
@Slf4j
@Service
public class UnionPriceIngestionServiceV2Impl implements UnionPriceIngestionService {

    @Override
    public boolean startSaveAllPricesToUnion() {
        return omittedFalse();
    }

    @Override
    public void saveAllPricesToUnion() {
        omitted();
    }

    @Override
    public void saveOpenBinderPricesToUnion() {
        omitted();
    }

    @Override
    public void saveFabPricesToUnion(List<FabPrice> fabPrices) {
        omitted();
    }

    @Override
    public void saveMtgPricesToUnion(List<MtgPrice> mtgPrices) {
        omitted();
    }

    @Override
    public void saveTcgPricesToUnion(List<TcgPPrice> tcgPrices) {
        omitted();
    }

    @Override
    public void saveTcgPricesImageUrlToUnion(List<TcgPPrice> tcgPrices) {
        omitted();
    }

    @Override
    public void saveTcgPriceImageUrlToUnion(TcgPPrice tcgPrice) {
        omitted();
    }

    @Override
    public int backfillMissingPublicIds() {
        omitted();
        return 0;
    }

    @Override
    public boolean backfillProductSearchMapFromUnionPrices() {
        return omittedFalse();
    }

    @Override
    public boolean rebuildProductSearchMapsByReferenceAxis() {
        return omittedFalse();
    }

    @Override
    public ProductSearchMapRebuildStatus getProductSearchMapRebuildStatus() {
        return new ProductSearchMapRebuildStatus(false, "omitted", 0, 0L, "omitted in public snapshot");
    }

    private static void omitted() {
        log.warn("Portfolio snapshot: union price ingestion is omitted.");
    }

    private static boolean omittedFalse() {
        omitted();
        return false;
    }
}
