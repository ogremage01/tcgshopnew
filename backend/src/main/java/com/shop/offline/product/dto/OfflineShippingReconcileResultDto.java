package com.shop.offline.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오프라인 출고(shippingQuantity) 전량 재집계 결과.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineShippingReconcileResultDto {

    /** shipping_quantity 값이 바뀐 상품 수 */
    private int updatedProductCount;
    /** COMPLETED 매출에 등장한 title 수 */
    private int salesTitleCount;
    /** 매출 title 중 OfflineProduct 미매칭 수 */
    private int unmatchedSalesTitleCount;
    /** OfflineProduct 동일 title 복수 그룹 수 */
    private int duplicateTitleGroupCount;
}
