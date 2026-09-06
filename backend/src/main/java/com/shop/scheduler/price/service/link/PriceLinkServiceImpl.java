package com.shop.scheduler.price.service.link;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 소스 가격 간 매칭·링크 SQL은 포함하지 않습니다.
 */
@Slf4j
@Service
public class PriceLinkServiceImpl implements PriceLinkService {

    @Override
    public void syncMtgPriceWithTcgPrice() {
        omitted();
    }

    @Override
    public void syncFabPriceWithTcgPrice() {
        omitted();
    }

    @Override
    public void backfillTcgObPriceIdFromMtgLinks() {
        omitted();
    }

    @Override
    public void backfillTcgObPriceIdFromFabLinks() {
        omitted();
    }

    @Override
    public boolean startSyncPriceLink() {
        return omittedFalse();
    }

    @Override
    public boolean startSyncPriceLinkRescanNulls() {
        return omittedFalse();
    }

    @Override
    public void syncMtgPriceWithTcgPriceRescanNulls() {
        omitted();
    }

    @Override
    public void syncFabPriceWithTcgPriceRescanNulls() {
        omitted();
    }

    @Override
    public boolean isSyncPriceLinkRunning() {
        return false;
    }

    @Override
    public int runSelectThenUpdate(String selectSql, String updateSql) {
        omitted();
        return 0;
    }

    @Override
    public <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || list.isEmpty() || size <= 0) {
            return List.of();
        }
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    @Override
    public boolean startRebuildCheckCodes() {
        return omittedFalse();
    }

    @Override
    public boolean startRebuildCheckCodesSelective() {
        return omittedFalse();
    }

    @Override
    public boolean startRebuildLinks() {
        return omittedFalse();
    }

    @Override
    public boolean startFullRebuild() {
        return omittedFalse();
    }

    private static void omitted() {
        log.warn("Portfolio snapshot: price-link matching is omitted.");
    }

    private static boolean omittedFalse() {
        omitted();
        return false;
    }
}
