package com.shop.scheduler.price.application.selling;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 판매가 일괄 재계산 규칙은 포함하지 않습니다.
 */
@Slf4j
@Service
public class CardSellingPriceUpdateServiceImpl implements CardSellingPriceUpdateService {

    @Override
    public boolean startUpdateCardCalculatedLinkedPrice() {
        return omittedFalse();
    }

    @Override
    public boolean startUpdateCardCalculatedLinkedPriceForGrade(String grade) {
        return omittedFalse();
    }

    @Override
    public void updateCardCalculatedLinkedPrice() {
        omitted();
    }

    @Override
    public void updateCardCalculatedLinkedPriceForGrade(String grade) {
        omitted();
    }

    @Override
    public void raiseMinimumPriceForCardProducts(Long minimumPrice, String game) {
        omitted();
    }

    @Override
    public void dropMinimumPriceForCardProducts(Long newMinimumPrice, Long lastMinimumPrice, String game) {
        omitted();
    }

    private static void omitted() {
        log.warn("Portfolio snapshot: card selling price update is omitted.");
    }

    private static boolean omittedFalse() {
        omitted();
        return false;
    }
}
