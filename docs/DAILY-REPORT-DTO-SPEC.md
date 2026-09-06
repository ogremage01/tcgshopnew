# AdminDailyReportDto 계산 명세

일별 판매 레포트(`getDailySalesReport`) API가 반환하는 각 DTO 필드가  
어떤 테이블·컬럼에서 어떻게 계산되는지 정리한 문서입니다.

---

## 오프라인 안분(按分) 계산 방식

오프라인 주문 1건에 현금·카드 등 **복수 결제 수단**이 사용될 수 있습니다.  
이 경우 **정가(grossAmount)** 와 **할인액(discountAmount)** 은 결제 비율로 배분합니다.

```
비율(ratio) = 해당 결제 수단 결제액 ÷ 주문 총금액
배분 정가   = list_price × ratio
배분 할인   = 할인액 × ratio
```

할인액 우선순위: `offline_sales_item_discounts.amount` (아이템 단위) → 없으면 `offline_sales_infos.discount_amount` (주문 헤더)

---

## 1. 총계

온라인 + 오프라인 합산

| DTO 필드              | 설명                   | 채널              | 소스 테이블 · 컬럼                                                                   | 계산 방식                                                        |
| --------------------- | ---------------------- | ----------------- | ------------------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| `totalOrderCount`     | 총 주문 건수           | 온라인 + 오프라인 | `order_infos` / `offline_sales_infos`                                                | 각 채널 COUNT(\*) 합산                                           |
| `totalOrderAmount`    | 총 주문 금액 (할인 전) | 온라인 + 오프라인 | `order_infos.total_product_amount` / `offline_sales_infos.list_price`                | SUM 합산                                                         |
| `totalUsedPoint`      | 총 사용 적립금         | **온라인만**      | `order_infos.used_point_amount`                                                      | SUM                                                              |
| `totalDiscountAmount` | 총 할인 금액           | **오프라인만**    | `offline_sales_item_discounts.amount` (우선) / `offline_sales_infos.discount_amount` | 아이템 할인 있으면 우선, 없으면 헤더 discount_amount 사용 후 SUM |
| `totalPaymentAmount`  | 총 결제 금액           | 온라인 + 오프라인 | `order_infos.actual_payment_amount` / `offline_sales_payments.amount`                | 각 채널 SUM 합산                                                 |

---

## 2. 온라인 — 결제 수단별

`order_infos.payment_method` 기준으로 **DIRECT** / **ONLINE_CARD** / **ONLINE_EASY_PAY** / **ONLINE_OTHER** 4개 그룹으로 분류합니다.

- `DIRECT`: `payment_method = 'DIRECT'` (매장결제)
- `ONLINE_CARD`: `payment_method = '카드'`
- `ONLINE_EASY_PAY`: `payment_method = '간편결제'`
- `ONLINE_OTHER`: 그 외 (`TOSS` 폴백, `가상계좌` 등). **DTO/화면에는 노출하지 않고**, 총계(`totalOrderCount` 등)에만 포함한다.

- 주문 건수·포인트·실결제금액: `order_infos`에서 집계
- 상품 합계 금액(orderAmount): `order_products.total_price`에서 별도 집계 후 병합

| 결제 그룹        | DTO 필드                           | 설명             | 소스 테이블 · 컬럼                  | 계산 방식                                 |
| ---------------- | ---------------------------------- | ---------------- | ----------------------------------- | ----------------------------------------- |
| DIRECT           | `totalOnlineStoreOrderCount`       | 주문 건수        | `order_infos`                       | `payment_method = 'DIRECT'` COUNT(\*)     |
| DIRECT           | `totalOnlineStoreOrderAmount`      | 상품 합계 금액   | `order_products.total_price`        | 해당 그룹 주문 SUM(total_price)           |
| DIRECT           | `totalOnlineStoreUsedPoint`        | 포인트 사용 합계 | `order_infos.used_point_amount`     | SUM                                       |
| DIRECT           | `totalOnlineStorePaymentAmount`    | 실결제 금액      | `order_infos.actual_payment_amount` | SUM                                       |
| ONLINE_CARD      | `totalOnlineCardOrderCount`        | 주문 건수        | `order_infos`                       | `payment_method = '카드'` COUNT(\*)       |
| ONLINE_CARD      | `totalOnlineCardOrderAmount`       | 상품 합계 금액   | `order_products.total_price`        | 해당 그룹 주문 SUM(total_price)           |
| ONLINE_CARD      | `totalOnlineCardUsedPoint`         | 포인트 사용 합계 | `order_infos.used_point_amount`     | SUM                                       |
| ONLINE_CARD      | `totalOnlineCardPaymentAmount`     | 실결제 금액      | `order_infos.actual_payment_amount` | SUM                                       |
| ONLINE_EASY_PAY  | `totalOnlineEasyPayOrderCount`     | 주문 건수        | `order_infos`                       | `payment_method = '간편결제'` COUNT(\*)   |
| ONLINE_EASY_PAY  | `totalOnlineEasyPayOrderAmount`    | 상품 합계 금액   | `order_products.total_price`        | 해당 그룹 주문 SUM(total_price)           |
| ONLINE_EASY_PAY  | `totalOnlineEasyPayUsedPoint`      | 포인트 사용 합계 | `order_infos.used_point_amount`     | SUM                                       |
| ONLINE_EASY_PAY  | `totalOnlineEasyPayPaymentAmount`  | 실결제 금액      | `order_infos.actual_payment_amount` | SUM                                       |
| ONLINE_OTHER     | (DTO 없음, 총계에만 합산)          | 그 외 결제 수단  | `order_infos` / `order_products`    | DIRECT/카드/간편결제가 아닌 주문          |

---

## 3. 온라인 — 상품 분류별

`order_products.product_table` 기준으로 4개 분류로 나눕니다.  
`product_table`이 NULL인 행은 `UNION_PRICE`로 처리되어 기타에 포함됩니다.  
`categoryCount`는 `COUNT(DISTINCT product_id)` 기준입니다.

| 분류     | product_table 값                                            | DTO 필드                                     | 설명             | 계산 방식                               |
| -------- | ----------------------------------------------------------- | -------------------------------------------- | ---------------- | --------------------------------------- |
| 싱글카드 | `CARD_PRODUCT`                                              | `totalOnlineSingleCardOrderCategoryCount`    | 고유 상품 종수   | COUNT(DISTINCT product_id)              |
| 싱글카드 | `CARD_PRODUCT`                                              | `totalOnlineSingleCardOrderQuantity`         | 판매 수량 합계   | SUM(quantity)                           |
| 싱글카드 | `CARD_PRODUCT`                                              | `totalOnlineSingleCardOrderAmount`           | 판매 금액 합계   | SUM(total_price)                        |
| 봉입상품 | `SEALED_PRODUCT`                                            | `totalOnlineSealedProductOrderCategoryCount` | 고유 상품 종수   | COUNT(DISTINCT product_id)              |
| 봉입상품 | `SEALED_PRODUCT`                                            | `totalOnlineSealedProductOrderQuantity`      | 판매 수량 합계   | SUM(quantity)                           |
| 봉입상품 | `SEALED_PRODUCT`                                            | `totalOnlineSealedProductOrderAmount`        | 판매 금액 합계   | SUM(total_price)                        |
| 서플라이 | `SUPPLY`                                                    | `totalOnlineSupplyOrderCount`                | 고유 상품 종수   | COUNT(DISTINCT product_id)              |
| 서플라이 | `SUPPLY`                                                    | `totalOnlineSupplyOrderQuantity`             | 판매 수량 합계   | SUM(quantity)                           |
| 서플라이 | `SUPPLY`                                                    | `totalOnlineSupplyOrderAmount`               | 판매 금액 합계   | SUM(total_price)                        |
| 기타     | `MANUAL_PRODUCT` / `UNION_PRICE` / `OTHER_PRODUCT` / 미분류 | `totalOnlineOtherProductOrderCategoryCount`  | 고유 상품 종수   | COUNT(DISTINCT product_id)              |
| 기타     | (위와 동일)                                                 | `totalOnlineOtherProductOrderQuantity`       | 판매 수량 합계   | SUM(quantity)                           |
| 기타     | (위와 동일)                                                 | `totalOnlineOtherProductAmount`              | 판매 금액 합계   | SUM(total_price)                        |
| **소계** | 전체                                                        | `totalOnlineTotalAmount`                     | 온라인 상품 합계 | SUM(order_products.total_price) 전 상품 |

---

## 4. 오프라인 — 결제 수단별

`offline_sales_payments.source_type` 기준으로 3개 그룹으로 분류합니다.  
`grossAmount`·`discountAmount`는 [안분 계산](#오프라인-안분按分-계산-방식)으로 산출되고,  
`paymentAmount`는 실제 결제 금액을 직접 합산합니다.

| 결제 그룹      | source_type              | DTO 필드                                 | 설명                    | 계산 방식                                                |
| -------------- | ------------------------ | ---------------------------------------- | ----------------------- | -------------------------------------------------------- |
| CASH           | `CASH`                   | `totalOfflineCashCount`                  | 결제 건수               | COUNT(offline_sales_payments) where source_type = CASH   |
| CASH           | `CASH`                   | `totalOfflineCashAmount`                 | 정가 (안분)             | list_price × (현금결제액 / 주문총액) 안분 합산           |
| CASH           | `CASH`                   | `totalOfflineCashDiscountAmount`         | 할인액 (안분)           | item_discount(없으면 order_discount) × ratio 안분 합산   |
| CASH           | `CASH`                   | `totalOfflineCashPaymentAmount`          | 현금 실결제 금액        | SUM(offline_sales_payments.amount) where CASH            |
| CARD / BARCODE | `CARD`, `BARCODE`        | `totalOfflineCardOrderCount`             | 결제 건수               | COUNT where source_type IN (CARD, BARCODE)               |
| CARD / BARCODE | `CARD`, `BARCODE`        | `totalOfflineCardOrderAmount`            | 정가 (안분)             | list_price × (카드/바코드 결제액 / 주문총액) 안분 합산   |
| CARD / BARCODE | `CARD`, `BARCODE`        | `totalOfflineCardDiscountAmount`         | 할인액 (안분)           | item_discount(없으면 order_discount) × ratio 안분 합산   |
| CARD / BARCODE | `CARD`, `BARCODE`        | `totalOfflineCardPaymentAmount`          | 카드/바코드 실결제 금액 | SUM(offline_sales_payments.amount) where CARD or BARCODE |
| 기타 결제      | CASH / CARD / BARCODE 외 | `totalOfflineOtherPaymentCount`          | 결제 건수               | COUNT 나머지 source_type                                 |
| 기타 결제      | (위와 동일)              | `totalOfflineOtherPaymentAmount`         | 정가 (안분)             | list_price × (기타결제액 / 주문총액) 안분 합산           |
| 기타 결제      | (위와 동일)              | `totalOfflineOtherPaymentDiscountAmount` | 할인액 (안분)           | item_discount(없으면 order_discount) × ratio 안분 합산   |
| 기타 결제      | (위와 동일)              | `totalOfflineOtherPaymentPaymentAmount`  | 기타 실결제 금액        | SUM(offline_sales_payments.amount) 나머지 source_type    |

---

## 5. 오프라인 — 상품 분류별

`offline_products.link_table_name` 기준으로 3개 분류로 나눕니다.  
`offline_products` 마스터에 없는 상품(링크 없음)은 **Manual**로 처리됩니다.  
`paymentAmount = grossAmount − discountAmount` (음수 방지 0 처리).

| 분류          | link_table_name         | DTO 필드                                      | 설명           | 계산 방식                                    |
| ------------- | ----------------------- | --------------------------------------------- | -------------- | -------------------------------------------- |
| 봉입상품      | `Sealed`                | `totalOfflineSealedProductOrderCategoryCount` | 고유 상품 종수 | COUNT(DISTINCT offline_sales_items.title)    |
| 봉입상품      | `Sealed`                | `totalOfflineSealedProductOrderQuantity`      | 판매 수량 합계 | SUM(quantity)                                |
| 봉입상품      | `Sealed`                | `totalOfflineSealedProductOrderAmount`        | 정가 합계      | SUM(price_value × quantity)                  |
| 봉입상품      | `Sealed`                | `totalOfflineSealedProductDiscountAmount`     | 할인 합계      | SUM(offline_sales_item_discounts.amount)     |
| 봉입상품      | `Sealed`                | `totalOfflineSealedProductPaymentAmount`      | 실결제 합계    | gross − discount (음수 방지 0 처리)          |
| 서플라이      | `Supply`                | `totalOfflineSupplyOrderCount`                | 고유 상품 종수 | COUNT(DISTINCT offline_sales_items.title)    |
| 서플라이      | `Supply`                | `totalOfflineSupplyOrderQuantity`             | 판매 수량 합계 | SUM(quantity)                                |
| 서플라이      | `Supply`                | `totalOfflineSupplyOrderAmount`               | 정가 합계      | SUM(price_value × quantity)                  |
| 서플라이      | `Supply`                | `totalOfflineSupplyDiscountAmount`            | 할인 합계      | SUM(offline_sales_item_discounts.amount)     |
| 서플라이      | `Supply`                | `totalOfflineSupplyPaymentAmount`             | 실결제 합계    | gross − discount (음수 방지 0 처리)          |
| 기타 (Manual) | Sealed / Supply 외 전체 | `totalOfflineOtherProductCount`               | 고유 상품 종수 | COUNT(DISTINCT offline_sales_items.title)    |
| 기타 (Manual) | (위와 동일)             | `totalOfflineOtherProductQuantity`            | 판매 수량 합계 | SUM(quantity)                                |
| 기타 (Manual) | (위와 동일)             | `totalOfflineOtherProductAmount`              | 정가 합계      | SUM(price_value × quantity)                  |
| 기타 (Manual) | (위와 동일)             | `totalOfflineOtherProductDiscountAmount`      | 할인 합계      | SUM(offline_sales_item_discounts.amount)     |
| 기타 (Manual) | (위와 동일)             | `totalOfflineOtherProductPaymentAmount`       | 실결제 합계    | gross − discount (음수 방지 0 처리)          |
| **소계**      | 전체                    | `totalOfflineTotalAmount`                     | 정가 소계      | Sealed + Supply + Manual grossAmount 합산    |
| **소계**      | 전체                    | `totalOfflineTotalDiscountAmount`             | 할인 소계      | Sealed + Supply + Manual discountAmount 합산 |
| **소계**      | 전체                    | `totalOfflineTotalPaymentAmount`              | 실결제 소계    | Sealed + Supply + Manual paymentAmount 합산  |
