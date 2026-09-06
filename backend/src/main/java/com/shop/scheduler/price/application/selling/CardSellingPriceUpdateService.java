package com.shop.scheduler.price.application.selling;

public interface CardSellingPriceUpdateService {

    boolean startUpdateCardCalculatedLinkedPrice();

    boolean startUpdateCardCalculatedLinkedPriceForGrade(String grade);

    void updateCardCalculatedLinkedPrice();

    void updateCardCalculatedLinkedPriceForGrade(String grade);

    void raiseMinimumPriceForCardProducts(Long minimumPrice, String game);

    void dropMinimumPriceForCardProducts(Long newMinimumPrice, Long lastMinimumPrice, String game);
}
