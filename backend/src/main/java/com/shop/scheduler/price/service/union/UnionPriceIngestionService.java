package com.shop.scheduler.price.service.union;

import java.util.List;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

/** 소스 가격 테이블 → {@code union_prices} 적재·동기화 (스케줄러·배치). */
public interface UnionPriceIngestionService {

    boolean startSaveAllPricesToUnion();

    void saveAllPricesToUnion();

    /**
     * Open Binder(mtg_prices·fab_prices)만 union_prices·ProductSearchMap에 반영한다.
     * 일일 배치는 {@link #saveAllPricesToUnion()}을 사용한다 (TCG 포함).
     */
    void saveOpenBinderPricesToUnion();

    void saveFabPricesToUnion(List<FabPrice> fabPrices);

    void saveMtgPricesToUnion(List<MtgPrice> mtgPrices);

    void saveTcgPricesToUnion(List<TcgPPrice> tcgPrices);

    void saveTcgPricesImageUrlToUnion(List<TcgPPrice> tcgPrices);

    void saveTcgPriceImageUrlToUnion(TcgPPrice tcgPrice);

    int backfillMissingPublicIds();

    /** 기존 union_prices 전체를 ProductSearchMap에 비동기로 동기화한다. 이미 실행 중이면 false를 반환한다. */
    boolean backfillProductSearchMapFromUnionPrices();

    boolean rebuildProductSearchMapsByReferenceAxis();

    ProductSearchMapRebuildStatus getProductSearchMapRebuildStatus();

}
