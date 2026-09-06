# ProductSearchMap 참조축 정리 리팩터링 보고서

## 1. 목적

이 문서는 `ProductSearchMap.tableName`의 의미를 “이 row가 최종적으로 참조해야 하는 실제 테이블”로 재정의한 리팩터링 내용을 정리한 보고서입니다.

기존에는 카드 카탈로그 row와 카드 판매 row가 모두 `UNION_PRICE`를 사용할 수 있었습니다. 이 때문에 실제 `card_product.current_visible_stock`은 정상인데, 검색맵의 `inStock`은 다른 row에 남아 있는 값 때문에 `false`로 보이는 문제가 발생할 수 있었습니다.

이번 변경은 “표시용 대표 row”와 “실제 판매 row”를 명확히 분리하는 것이 핵심입니다.

## 2. 요약

| 영역 | 새 규칙 |
| --- | --- |
| 카드 카탈로그 대표 row | `tableName = UNION_PRICE` |
| 싱글 카드 판매 row | `tableName = CARD_PRODUCT` |
| sealed 상품 row | `tableName = SEALED_PRODUCT` |
| 수동 상품 row | `tableName = MANUAL_PRODUCT` |
| 판매용 `searchMapId` | 반드시 `CARD_PRODUCT`, `SEALED_PRODUCT`, `MANUAL_PRODUCT` 중 하나를 가리킴 |
| 장바구니 / 결제 / 주문 재고 대상 | `ProductSearchMap.tableName` 기준으로 결정 |

`UNION_PRICE`는 이제 카드 카탈로그 표시용 row입니다. 장바구니, 결제, 주문 재고 차감의 대상이 아닙니다.

## 3. ProductSearchMap 필드 의미

| 필드 | 의미 |
| --- | --- |
| `tableName` | 이 row가 실제로 참조하는 테이블 |
| `sourceId` | `tableName`이 가리키는 테이블의 PK |
| `sourcePublicId` | `tableName`이 가리키는 테이블의 public ID |
| `productId` | 이 검색맵 row의 public 식별자 |
| `catalogSourceId` | 카드 판매 row를 카드 카탈로그 대표 row와 묶기 위한 키 |
| `inStock` | 이 row 기준의 재고 보유 여부 캐시 |
| `isVisible` | 이 row가 노출 또는 사용 가능한지 여부 |

`catalogSourceId`는 카드 상품에서만 중요한 연결키입니다. 카드의 경우 `union_prices.id`가 들어갑니다.

## 4. Row 유형별 구조

### 4.1 카드 카탈로그 대표 row

| 필드 | 값 |
| --- | --- |
| `tableName` | `UNION_PRICE` |
| `sourceId` | `union_prices.id` |
| `sourcePublicId` | `union_prices.public_id` |
| `productId` | `union_prices.public_id` |
| `catalogSourceId` | `union_prices.id` |
| 역할 | 검색 목록과 상세의 카드 대표 row |

이 row는 `UnionPrice` 기준으로 생성 또는 갱신됩니다.

### 4.2 카드 판매 row

| 필드 | 값 |
| --- | --- |
| `tableName` | `CARD_PRODUCT` |
| `sourceId` | `card_product.id` |
| `sourcePublicId` | `card_product.public_id` |
| `productId` | `card_product.public_id` |
| `catalogSourceId` | `card_product.union_price_id` |
| 역할 | 판매 옵션, 장바구니 항목, 결제 라인, 재고 차감 대상 |

이 row는 `CardProduct` 기준으로 생성 또는 갱신됩니다.

### 4.3 Sealed 상품 row

| 필드 | 값 |
| --- | --- |
| `tableName` | `SEALED_PRODUCT` |
| `sourceId` | `sealed_product.id` |
| `sourcePublicId` | `sealed_product.public_id` |
| `productId` | `sealed_product.public_id` |
| `catalogSourceId` | `null` |
| 역할 | sealed 상품의 검색, 상세, 판매 row |

sealed 상품은 `union_prices`와 연결하지 않습니다. 관리자가 참조 데이터를 불러와 셀을 채울 수는 있지만, 실제 판매 row는 `sealed_product`만 직접 참조합니다.

### 4.4 수동 상품 row

| 필드 | 값 |
| --- | --- |
| `tableName` | `MANUAL_PRODUCT` |
| `sourceId` | `manual_product.id` |
| `sourcePublicId` | `manual_product.public_id` |
| `productId` | `manual_product.public_id` |
| `catalogSourceId` | `null` |
| 역할 | 수동 상품의 검색, 상세, 판매 row |

## 5. 연결 구조

### 5.1 카드 카탈로그 row와 판매 row 연결

```text
union_prices
  id = 101
  public_id = UP...

product_search_maps (카탈로그 대표 row)
  table_name = UNION_PRICE
  source_id = 101
  product_id = UP...
  catalog_source_id = 101

card_product
  id = 501
  public_id = CP...
  union_price_id = 101

product_search_maps (카드 판매 row)
  table_name = CARD_PRODUCT
  source_id = 501
  product_id = CP...
  catalog_source_id = 101
```

카드 카탈로그 row와 카드 판매 row는 `catalogSourceId = union_prices.id`로 묶입니다.

### 5.2 Sealed 상품 직접 연결

```text
sealed_product
  id = 701
  public_id = SP...

product_search_maps
  table_name = SEALED_PRODUCT
  source_id = 701
  product_id = SP...
  catalog_source_id = null
```

sealed 상품은 `union_prices`를 거치지 않습니다.

### 5.3 수동 상품 직접 연결

```text
manual_product
  id = 801
  public_id = MP...

product_search_maps
  table_name = MANUAL_PRODUCT
  source_id = 801
  product_id = MP...
  catalog_source_id = null
```

## 6. 주요 데이터 흐름

### 6.1 UnionPrice 동기화

```text
UnionPrice 수집 / 백필
  -> ProductSearchMapService.syncProductSearchMap(UnionPrice)
  -> upsertByUnionPrice
  -> tableName = UNION_PRICE row 생성 또는 갱신
```

`UNION_PRICE` row는 카드 검색 목록과 상세 대표 정보를 제공합니다.

### 6.2 CardProduct 동기화

```text
CardProduct 생성 / 수정 / 엑셀 업로드 / 재고 동기화
  -> ProductSearchMapService.syncProductSearchMap(CardProduct)
  -> upsertByCardProduct
  -> tableName = CARD_PRODUCT row 생성 또는 갱신
  -> syncUnionPriceAggregateSearchMap
  -> 연결된 UNION_PRICE 대표 row의 inStock 집계 갱신
```

`CARD_PRODUCT` row는 실제 판매 옵션과 재고 차감 대상을 담당합니다. 같은 `unionPrice`에 속한 판매 row 중 재고가 있는 row가 있으면, `UNION_PRICE` 대표 row의 `inStock`도 `true`로 갱신됩니다.

### 6.3 SealedProduct 동기화

```text
SealedProduct 생성 / 수정 / 재고 동기화
  -> ProductSearchMapService.syncProductSearchMap(SealedProduct)
  -> upsertBySealedProduct
  -> tableName = SEALED_PRODUCT row 생성 또는 갱신
```

sealed 상품은 독립 판매 상품이므로 `UNION_PRICE` 대표 row를 만들지 않습니다.

### 6.4 ManualProduct 동기화

```text
ManualProduct 생성 / 수정 / 재고 동기화
  -> ProductSearchMapService.syncProductSearchMap(ManualProduct)
  -> upsertByManualProduct
  -> tableName = MANUAL_PRODUCT row 생성 또는 갱신
```

## 7. 검색과 DTO 규칙

| 흐름 | 규칙 |
| --- | --- |
| 검색 목록 | `CARD_PRODUCT` row를 직접 노출하지 않음 |
| 카드 상세 | `UNION_PRICE` 대표 row에서 시작하고, 연결된 `CardProduct` 판매 옵션을 조회 |
| 카드 판매 DTO | 각 옵션의 `searchMapId`는 `CARD_PRODUCT` row ID |
| sealed 상세 | `SEALED_PRODUCT` row를 직접 사용 |
| sealed 판매 DTO | `searchMapId`는 `SEALED_PRODUCT` row ID |
| 수동 상품 상세 | `MANUAL_PRODUCT` row를 직접 사용 |

`ProductItemDto.table`은 현재 DTO의 대표 row 기준을 나타냅니다. 실제 장바구니에 담기는 판매 옵션 DTO의 `searchMapId`는 판매 row를 가리켜야 합니다.

## 8. 장바구니 / 결제 / 주문 규칙

장바구니와 결제는 판매 row만 허용합니다.

| 상품 유형 | 허용되는 `tableName` |
| --- | --- |
| 카드 | `CARD_PRODUCT` |
| sealed | `SEALED_PRODUCT` |
| 수동 상품 | `MANUAL_PRODUCT` |

`UNION_PRICE`는 장바구니 또는 결제 대상이 아닙니다.

재고 차감과 복구는 `productTable` 기준으로 라우팅됩니다.

| `productTable` | repository 대상 |
| --- | --- |
| `CARD_PRODUCT` | `CardProductRepository` |
| `SEALED_PRODUCT` | `SealedProductRepository` |
| `MANUAL_PRODUCT` | `ManualProductRepository` |

## 9. 파일 연결표

| 파일 | 역할 |
| --- | --- |
| `ProductTableEnum.java` | `CARD_PRODUCT`, `SEALED_PRODUCT` 추가 |
| `ProductSearchMap.java` | `catalogSourceId` 필드와 인덱스 추가 |
| `ProductSearchMapRepository.java` | `catalogSourceId` 조회 메서드 추가 |
| `ProductSearchMapRepositoryImpl.java` | 검색 목록에서 `CARD_PRODUCT` row 제외 |
| `ProductSearchMapService.java` | `SealedProduct` 동기화 계약 추가 |
| `ProductSearchMapServiceImpl.java` | row 생성, 상세 조회, 판매 DTO 해석 담당 |
| `ProductSearchMapStockSyncPublisher.java` | sealed 재고 동기화 publisher 추가 |
| `ProductItemDtoMapper.java` | 판매 DTO의 `searchMapId`를 실제 판매 row로 매핑 |
| `CartServiceImpl.java` | 장바구니에서 판매 row만 허용 |
| `CheckoutDraftServiceImpl.java` | 결제 초안에서 판매 row만 허용 |
| `UnionPriceCheckoutLineHandler.java` | 카드 판매 라인을 `CARD_PRODUCT` 기준으로 처리 |
| `SealedProductCheckoutLineHandler.java` | sealed 판매 라인을 `SEALED_PRODUCT` 기준으로 처리 |
| `CheckoutConfirmServiceImpl.java` | 결제 확정 후 table별 재고 동기화 발행 |
| `AdminOrderServiceImpl.java` | 주문 취소 시 table별 재고 복구 |
| `SealedProductRepository.java` | sealed 재고 차감 / 복구 메서드 추가 |
| `product_search_maps_catalog_source_id.sql` | `catalog_source_id` 수동 DDL |

## 10. 스키마 변경

`product_search_maps`에 아래 컬럼이 추가되었습니다.

```sql
catalog_source_id BIGINT NULL
```

수동 스키마 환경에서는 아래 스크립트를 적용합니다.

```text
backend/script/product_search_maps_catalog_source_id.sql
```

개발 환경처럼 `spring.jpa.hibernate.ddl-auto=update`를 사용하는 경우에는 Hibernate가 컬럼을 자동 생성할 수 있습니다.

## 11. 기대 동작 점검표

| 시나리오 | 기대 결과 |
| --- | --- |
| 엑셀로 카드 업로드, `current_visible_stock > 0` | `CARD_PRODUCT.inStock = true`, 연결된 `UNION_PRICE.inStock = true` |
| 카드 검색 목록 조회 | `UNION_PRICE` 대표 row 1건 기준으로 노출 |
| 카드 상세 조회 | 판매 옵션의 `searchMapId`가 `CARD_PRODUCT` row를 가리킴 |
| 카드 장바구니 추가 | `CARD_PRODUCT` searchMapId만 허용 |
| sealed 장바구니 추가 | `SEALED_PRODUCT` searchMapId만 허용 |
| 결제 확정 | 실제 판매 테이블에서 재고 차감 |
| 주문 취소 재고 복구 | 실제 판매 테이블로 재고 복구 |

## 12. 검증

리팩터링 후 백엔드 컴파일을 수행했습니다.

```powershell
backend\gradlew.bat -p backend compileJava
```

결과: `BUILD SUCCESSFUL`

## 13. 참고 사항

- 이번 리팩터링은 개발 중인 데이터는 초기화 가능하다는 전제로 진행했습니다.
- 기존 `UNION_PRICE` 판매 row에 대한 런타임 호환 레이어는 두지 않습니다.
- sealed 상품 생성 / 수정 서비스가 연결될 때는 저장 후 `ProductSearchMapService.syncProductSearchMap(SealedProduct)`를 호출해야 합니다.
