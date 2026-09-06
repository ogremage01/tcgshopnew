package com.shop.admin.analyze.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDailyReportDto {

    //----------------------------- 총계 -----------------------------
    /**
     * 총 주문 수(온라인+오프라인)
     */
    private Long totalOrderCount;

    /**
     * 총 주문 금액(온라인(DIRECT 제외)+배송료 + 오프라인(싱글카드 포함))
     */
    private Long totalOrderAmount;

    /**
     * 총 사용 적립금(온라인. sum(ORDER_INFOS:used_point_amount))
     */
    private Long totalUsedPoint;

    /**
     * 총 할인 금액(오프라인. sum(offline_sales_item_discounts.amount) 또는 헤더 discount_amount)
     */
    private Long totalDiscountAmount;

    /**
     * 총 결제 금액(온라인+오프라인. sum(ORDER_INFOS:actual_payment_amount)+sum(OFFLINE_SALES_PAYMENT:amount))
     */
    private Long totalPaymentAmount;

    //----------------------------- 온라인-매장 매출 -----------------------------
    /**
     * 총 온라인-매장 주문 수(온라인. count(ORDER_INFOS:payment_method=DIRECT))
     */
    private Long totalOnlineStoreOrderCount;

    /**
     * 총 온라인-매장 주문 금액(온라인. sum(ORDER_PRODUCT:totalPrice) where payment_method=DIRECT)
     */
    private Long totalOnlineStoreOrderAmount;

    /**
     * 총 온라인-매장 사용 적립금(온라인. sum(ORDER_INFOS:used_point_amount) where payment_method=DIRECT)
     */
    private Long totalOnlineStoreUsedPoint;

    /**
     * 총 온라인-매장 결제 금액(온라인. sum(ORDER_INFOS:actual_payment_amount) where payment_method=DIRECT)
     */
    private Long totalOnlineStorePaymentAmount;

    //----------------------------- 온라인-카드결제 매출 -----------------------------
    /**
     * 총 온라인-카드결제 주문 수(온라인. count(ORDER_INFOS:payment_method='카드'))
     */
    private Long totalOnlineCardOrderCount;

    /**
     * 총 온라인-카드결제 주문 금액(온라인. sum(ORDER_PRODUCT:totalPrice) where payment_method='카드')
     */
    private Long totalOnlineCardOrderAmount;

    /**
     * 총 온라인-카드결제 사용 적립금(온라인. sum(ORDER_INFOS:used_point_amount) where payment_method='카드')
     */
    private Long totalOnlineCardUsedPoint;

    /**
     * 총 온라인-카드결제 결제 금액(온라인. sum(ORDER_INFOS:actual_payment_amount) where payment_method='카드')
     */
    private Long totalOnlineCardPaymentAmount;

    //----------------------------- 온라인-간편결제 매출 -----------------------------
    /**
     * 총 온라인-간편결제 주문 수(온라인. count(ORDER_INFOS:payment_method='간편결제'))
     */
    private Long totalOnlineEasyPayOrderCount;

    /**
     * 총 온라인-간편결제 주문 금액(온라인. sum(ORDER_PRODUCT:totalPrice) where payment_method='간편결제')
     */
    private Long totalOnlineEasyPayOrderAmount;

    /**
     * 총 온라인-간편결제 사용 적립금(온라인. sum(ORDER_INFOS:used_point_amount) where payment_method='간편결제')
     */
    private Long totalOnlineEasyPayUsedPoint;

    /**
     * 총 온라인-간편결제 결제 금액(온라인. sum(ORDER_INFOS:actual_payment_amount) where payment_method='간편결제')
     */
    private Long totalOnlineEasyPayPaymentAmount;

    //----------------------------- 온라인-싱글카드 매출 -----------------------------
    /**
     * 총 온라인-싱글카드 주문 종수 수(온라인. ORDER_PRODUCT:productTable:CARD_PRODUCT)
     */
    private Long totalOnlineSingleCardOrderCategoryCount;

    /**
     * 총 온라인-싱글카드 주문 수량(온라인. ORDER_PRODUCT:productTable:CARD_PRODUCT*quantity)
     */
    private Long totalOnlineSingleCardOrderQuantity;

    /**
     * 총 온라인-싱글카드 주문 금액(온라인. sum(ORDER_PRODUCT:productTable:CARD_PRODUCT*totalPrice))
     */
    private Long totalOnlineSingleCardOrderAmount;


    //----------------------------- 온라인-밀봉제품 매출 -----------------------------
    /**
     * 총 온라인-밀봉제품 주문 종수 수(온라인. ORDER_PRODUCT:productTable:SEALED_PRODUCT)
     */
    private Long totalOnlineSealedProductOrderCategoryCount;

    /**
     * 총 온라인-밀봉제품 주문 수량(온라인. ORDER_PRODUCT:productTable:SEALED_PRODUCT*quantity)
     */
    private Long totalOnlineSealedProductOrderQuantity;


    /**
     * 총 온라인-밀봉제품 주문 금액(온라인. sum(ORDER_PRODUCT:productTable:SEALED_PRODUCT*totalPrice))
     */
    private Long totalOnlineSealedProductOrderAmount;

    //----------------------------- 온라인-서플라이 매출 -----------------------------
    /**
     * 총 온라인-서플라이 주문 수(온라인. count(ORDER_PRODUCT:productTable:SUPPLY))
     */
    private Long totalOnlineSupplyOrderCount;

    /**
     * 총 온라인-서플라이 주문 금액(온라인. sum(ORDER_PRODUCT:productTable:SUPPLY*totalPrice))
     */
    private Long totalOnlineSupplyOrderAmount;

    /**
     * 총 온라인-서플라이 주문 수량(온라인. ORDER_PRODUCT:productTable:SUPPLY*quantity)
     */
    private Long totalOnlineSupplyOrderQuantity;


    //----------------------------- 온라인-기타제품 매출 -----------------------------
    /**
     * 총 온라인-기타제품 주문 종수 수(온라인. count(ORDER_PRODUCT:productTable:MANUAL_PRODUCT))
     */
    private Long totalOnlineOtherProductOrderCategoryCount;

    /**
     * 총 온라인-기타제품 주문 수량(온라인. ORDER_PRODUCT:productTable:MANUAL_PRODUCT*quantity)
     */
    private Long totalOnlineOtherProductOrderQuantity;

    /**
     * 총 온라인-기타제품 주문 금액(온라인. sum(ORDER_PRODUCT:productTable:MANUAL_PRODUCT*totalPrice))
     */
    private Long totalOnlineOtherProductAmount;
    /**
     * 총 온라인 배송 건수(온라인. count(ORDER_INFO:deliveryFee > 0))
     */
    private Long totalOnlineDeliveryCount;

    /**
     * 총 온라인 배송료(온라인. sum(ORDER_INFO:deliveryFee))
     */
    private Long totalOnlineDeliveryFee;


    //----------------------------- 온라인-소계 -----------------------------

    /**
     * 총 온라인 소계 주문 금액(온라인. sum(ORDER_PRODUCT:totalPrice) + 배송료)
     */
    private Long totalOnlineTotalAmount;

    //----------------------------- 오프라인 매출-현금 -----------------------------
    /**
     * 총 오프라인-현금 주문 수(오프라인. count(OFFLINE_SALES_PAYMENT:sourceType:CASH))
     */
    private Long totalOfflineCashCount;

    /**
     * 총 오프라인-현금 매출 금액(오프라인. 주문 list_price를 CASH 결제 비율로 안분)
     */
    private Long totalOfflineCashAmount;

    /**
     * 총 오프라인-현금 주문 할인 금액(오프라인. sum(offlineSalesItemDiscount.amount) where OFFLINE_SALES_PAYMENT:sourceType:CASH and offlineSalesItem.Id in offlineSalesItemDiscount.offlineSalesItem.id)
     */
    private Long totalOfflineCashDiscountAmount;

    /**
     * 총 오프라인-현금 결제 금액(오프라인. sum(OFFLINE_SALES_PAYMENT:amount))
     */
    private Long totalOfflineCashPaymentAmount;

    //----------------------------- 오프라인-카드/간편 매출 -----------------------------
    //- 카드 매출(PAYMENT_TYPE:CARD or BARCODE)
    /**
     * 총 오프라인-카드/간편 주문 수(오프라인. count(OFFLINE_SALES_PAYMENT:SOURCE_TYPE:CARD or BARCODE))
     */
    private Long totalOfflineCardOrderCount;

    /**
     * 총 오프라인-카드/간편 매출 금액(오프라인. 주문 list_price를 CARD/BARCODE 결제 비율로 안분)
     */
    private Long totalOfflineCardOrderAmount;

    /**
     * 총 오프라인-카드/간편 주문 할인 금액(오프라인. sum(offlineSalesItemDiscount.amount) where OFFLINE_SALES_PAYMENT:SOURCE_TYPE:CARD or BARCODE and offlineSalesItem.Id in offlineSalesItemDiscount.offlineSalesItem.id)
     */
    private Long totalOfflineCardDiscountAmount;

    /**
     * 총 오프라인-카드/간편 결제 금액(오프라인. sum(OFFLINE_SALES_PAYMENT:SOURCE_TYPE:CARD or BARCODE*amount))
     */
    private Long totalOfflineCardPaymentAmount;

    //----------------------------- 오프라인-기타결제 매출 -----------------------------
    //- 기타결제 매출(CASH, CARD, BARCODE 외 전부)
    /**
     * 총 오프라인-기타결제 주문 수(오프라인. count(OFFLINE_SALES_PAYMENT:SOURCE_TYPE:CASH, CARD, BARCODE 외 전부))
     */
    private Long totalOfflineOtherPaymentCount;

    /**
     * 총 오프라인-기타결제 매출 금액(오프라인. 주문 list_price를 기타 결제 비율로 안분)
     */
    private Long totalOfflineOtherPaymentAmount;

    /**
     * 총 오프라인-기타결제 주문 할인 금액(오프라인. sum(offlineSalesItemDiscount.amount) where OFFLINE_SALES_PAYMENT:SOURCE_TYPE:CASH, CARD, BARCODE 외 전부 and offlineSalesItem.Id in offlineSalesItemDiscount.offlineSalesItem.id)
     */
    private Long totalOfflineOtherPaymentDiscountAmount;

    /**
     * 총 오프라인-기타결제 결제 금액(오프라인. sum(OFFLINE_SALES_PAYMENT:SOURCE_TYPE:CASH, CARD, BARCODE 외 전부*amount))
     */
    private Long totalOfflineOtherPaymentPaymentAmount;

    //----------------------------- 오프라인-밀봉제품 매출 -----------------------------
    /**
     * 총 오프라인-밀봉제품 주문 종수 (select count(distinct offlineSalesItem.title) from offlineSalesItem where offlineSalesItem.title=offlineProduct.title and offlineProduct.linkTableName=Sealed)
     */
    private Long totalOfflineSealedProductOrderCategoryCount;

    /**
     * 총 오프라인-밀봉제품 주문 수량 (select sum(offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title=offlineProduct.title and offlineProduct.linkTableName=Sealed)
     */
    private Long totalOfflineSealedProductOrderQuantity;

    /**
     * 총 오프라인-밀봉제품 주문 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title=offlineProduct.title and offlineProduct.linkTableName=Sealed)
     */
    private Long totalOfflineSealedProductOrderAmount;

    /**
     * 총 오프라인-밀봉제품 주문 할인 금액 (select sum(offlineSalesItemDiscount.amount) from offlineSalesItemDiscount where offlineSalesItemDiscount.offlineSalesItem.id in (select offlineSalesItem.id from offlineSalesItem where offlineSalesItem.title=offlineProduct.title and offlineProduct.linkTableName=Sealed))
     */
    private Long totalOfflineSealedProductDiscountAmount;

    /**
     * 총 오프라인-밀봉제품 결제 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity-offlineSalesItemDiscount.amount) from offlineSalesItem where offlineSalesItem.title=offlineProduct.title and offlineProduct.linkTableName=Sealed)
     */
    private Long totalOfflineSealedProductPaymentAmount;
    
    //----------------------------- 오프라인-서플라이 매출 -----------------------------
    /**
     * 총 오프라인-서플라이 주문 수 (select count(distinct offlineSalesItem.title) from offlineSalesItem where offlineSalesItem.title=offlineSupply.title and offlineSupply.linkTableName=Supply)
     */
    private Long totalOfflineSupplyOrderCount;

    /**
     * 총 오프라인-서플라이 주문 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title=offlineSupply.title and offlineSupply.linkTableName=Supply)
     */
    private Long totalOfflineSupplyOrderAmount;

    /**
     * 총 오프라인-서플라이 주문 수량 (select sum(offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title=offlineSupply.title and offlineSupply.linkTableName=Supply)
     */
    private Long totalOfflineSupplyOrderQuantity;

    /**
     * 총 오프라인-서플라이 주문 할인 금액 (select sum(offlineSalesItemDiscount.amount) from offlineSalesItemDiscount where offlineSalesItemDiscount.offlineSalesItem.id in (select offlineSalesItem.id from offlineSalesItem where offlineSalesItem.title=offlineSupply.title and offlineSupply.linkTableName=Supply))
     */
    private Long totalOfflineSupplyDiscountAmount;

    /**
     * 총 오프라인-서플라이 결제 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity-offlineSalesItemDiscount.amount) from offlineSalesItem where offlineSalesItem.title=offlineSupply.title and offlineSupply.linkTableName=Supply)
     */
    private Long totalOfflineSupplyPaymentAmount;

    //----------------------------- 오프라인-싱글카드 매출 -----------------------------
    /**
     * 총 오프라인-싱글카드 주문 종수 (select count(distinct offlineSalesItem.title) from offlineSalesItem where offlineSalesItem.title='Single cards')
     */
    private Long totalOfflineSingleCardOrderCategoryCount;

    /**
     * 총 오프라인-싱글카드 주문 수량 (select sum(offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title='Single cards')
     */
    private Long totalOfflineSingleCardOrderQuantity;

    /**
     * 총 오프라인-싱글카드 주문 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title='Single cards')
     */
    private Long totalOfflineSingleCardOrderAmount;

    /**
     * 총 오프라인-싱글카드 주문 할인 금액 (select sum(offlineSalesItemDiscount.amount) from offlineSalesItemDiscount where offlineSalesItem.title='Single cards')
     */
    private Long totalOfflineSingleCardDiscountAmount;

    /**
     * 총 오프라인-싱글카드 결제 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity-offlineSalesItemDiscount.amount) from offlineSalesItem where offlineSalesItem.title='Single cards')
     */
    private Long totalOfflineSingleCardPaymentAmount;

    //----------------------------- 오프라인-기타제품 매출 -----------------------------
    /**
     * 총 오프라인-기타제품 주문 수 (select count(distinct offlineSalesItem.title) from offlineSalesItem where offlineSalesItem.title=offlineOtherProduct.title and offlineOtherProduct.linkTableName=Manual)
     */
    private Long totalOfflineOtherProductCount;

    /**
     * 총 오프라인-기타제품 주문 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title=offlineOtherProduct.title and offlineOtherProduct.linkTableName=Manual)
     */
    private Long totalOfflineOtherProductAmount;

    /**
     * 총 오프라인-기타제품 주문 수량 (select sum(offlineSalesItem.quantity) from offlineSalesItem where offlineSalesItem.title=offlineOtherProduct.title and offlineOtherProduct.linkTableName=Manual)
     */
    private Long totalOfflineOtherProductQuantity;

    /**
     * 총 오프라인-기타제품 주문 할인 금액 (select sum(offlineSalesItemDiscount.amount) from offlineSalesItemDiscount where offlineSalesItemDiscount.offlineSalesItem.id in (select offlineSalesItem.id from offlineSalesItem where offlineSalesItem.title=offlineOtherProduct.title and offlineOtherProduct.linkTableName=Manual))
     */
    private Long totalOfflineOtherProductDiscountAmount;

    /**
     * 총 오프라인-기타제품 결제 금액 (select sum(offlineSalesItem.priceValue*offlineSalesItem.quantity-offlineSalesItemDiscount.amount) from offlineSalesItem where offlineSalesItem.title=offlineOtherProduct.title and offlineOtherProduct.linkTableName=Manual)
     */
    private Long totalOfflineOtherProductPaymentAmount;

    //----------------------------- 오프라인-소계 -----------------------------
    /**
     * 총 오프라인 소계 주문 금액 (밀봉/서플라이/기타 + 싱글카드)
     */
    private Long totalOfflineTotalAmount;

    /**
     * 총 오프라인 소계 할인 금액 (링크 테이블 할인 + 싱글카드 할인)
     */
    private Long totalOfflineTotalDiscountAmount;

    /**
     * 총 오프라인 소계 결제 금액 (링크 테이블 결제 + 싱글카드 결제)
     */
    private Long totalOfflineTotalPaymentAmount;
}
