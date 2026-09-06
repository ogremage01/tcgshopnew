# ProductSearchMap 정리 실행 가이드

## 1. 목적

이번 정리는 `product_search_maps.table_name`의 의미를 “이 row가 실제로 참조하는 테이블”로 고정한 뒤, 기존에 섞여 있던 검색맵 row를 새 기준으로 다시 생성하기 위한 절차입니다.

핵심은 다음과 같습니다.

| row 종류 | `table_name` | 참조 대상 |
| --- | --- | --- |
| 카드 카탈로그 대표 row | `UNION_PRICE` | `union_prices.id` |
| 카드 판매 row | `CARD_PRODUCT` | `card_product.id` |
| sealed 상품 row | `SEALED_PRODUCT` | `sealed_product.id` |
| 수동 상품 row | `MANUAL_PRODUCT` | `manual_products.id` |

`UNION_PRICE`는 더 이상 판매 row가 아닙니다. 장바구니, 결제, 주문, 재고 차감은 반드시 `CARD_PRODUCT`, `SEALED_PRODUCT`, `MANUAL_PRODUCT` row의 `searchMapId`를 사용해야 합니다.

## 2. 정리 스크립트

실행 대상 스크립트는 아래 파일입니다.

```text
backend/script/product_search_maps_reference_axis_rebuild.sql
```

이 스크립트는 `product_search_maps`를 모두 삭제한 뒤 현재 소스 테이블 기준으로 다시 생성합니다.

생성 순서는 다음과 같습니다.

1. `union_prices`에서 카드 카탈로그 대표 `UNION_PRICE` row 생성
2. `card_product`에서 카드 판매 `CARD_PRODUCT` row 생성
3. `sealed_product`에서 sealed 직접 판매 `SEALED_PRODUCT` row 생성
4. `manual_products`에서 수동 상품 `MANUAL_PRODUCT` row 생성

## 3. 실행 전 주의사항

개발 환경 기준으로 작성된 정리 스크립트입니다. 기존 장바구니, 체크아웃 초안, 주문 라인이 과거 `searchMapId`를 들고 있으면 `product_search_maps` 재생성 후 더 이상 유효하지 않을 수 있습니다.

따라서 실행 전에 아래 데이터를 함께 초기화하는 것을 권장합니다.

| 영역 | 이유 |
| --- | --- |
| cart 관련 테이블 | 기존 `search_map_id`가 삭제될 수 있음 |
| checkout draft 관련 테이블 | 기존 판매 row 참조가 삭제될 수 있음 |
| order 관련 테이블 | 기존 주문 라인의 `productTable=UNION_PRICE` 의미가 사라짐 |

현재 개발 단계라면 “주문/장바구니/체크아웃 데이터 초기화 후 product_search_maps 재구축”이 가장 안전합니다.

## 4. 실행 후 확인 SQL

### 4.1 카드 판매 row 재고 확인

`card_product.current_visible_stock > 0`인데 `CARD_PRODUCT` 검색맵이 `in_stock=false`인 row가 없어야 합니다.

```sql
SELECT
    cp.id AS card_product_id,
    cp.public_id AS card_product_public_id,
    cp.current_visible_stock,
    psm.id AS search_map_id,
    psm.in_stock
FROM card_product cp
JOIN product_search_maps psm
  ON psm.table_name = 'CARD_PRODUCT'
 AND psm.source_id = cp.id
WHERE COALESCE(cp.current_visible_stock, 0) > 0
  AND psm.in_stock = FALSE;
```

### 4.2 카드 대표 row 집계 확인

연결된 visible 카드 판매 row 중 재고가 있는 row가 있는데, 대표 `UNION_PRICE` row가 `in_stock=false`이면 안 됩니다.

```sql
SELECT
    up.id AS union_price_id,
    up.public_id AS union_price_public_id,
    psm.id AS search_map_id,
    psm.in_stock
FROM union_prices up
JOIN product_search_maps psm
  ON psm.table_name = 'UNION_PRICE'
 AND psm.source_id = up.id
WHERE EXISTS (
    SELECT 1
    FROM card_product cp
    WHERE cp.union_price_id = up.id
      AND cp.is_visible = TRUE
      AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
      AND COALESCE(cp.current_visible_stock, 0) > 0
)
  AND psm.in_stock = FALSE;
```

### 4.3 카드 판매 row와 대표 row 연결 확인

`CARD_PRODUCT.catalog_source_id`는 연결된 `union_prices.id`와 같아야 합니다.

```sql
SELECT
    cp.id AS card_product_id,
    cp.union_price_id,
    psm.id AS search_map_id,
    psm.catalog_source_id
FROM card_product cp
JOIN product_search_maps psm
  ON psm.table_name = 'CARD_PRODUCT'
 AND psm.source_id = cp.id
WHERE psm.catalog_source_id <> cp.union_price_id
   OR psm.catalog_source_id IS NULL;
```

### 4.4 sealed row 직접 참조 확인

sealed 상품은 `union_prices`와 연결하지 않습니다. `catalog_source_id`는 `NULL`이어야 합니다.

```sql
SELECT
    sp.id AS sealed_product_id,
    sp.public_id,
    psm.id AS search_map_id,
    psm.catalog_source_id
FROM sealed_product sp
JOIN product_search_maps psm
  ON psm.table_name = 'SEALED_PRODUCT'
 AND psm.source_id = sp.id
WHERE psm.catalog_source_id IS NOT NULL;
```

## 5. 기대 결과

정리 후에는 같은 카드라도 row 역할이 명확히 분리됩니다.

```text
union_prices.id = 101
  -> product_search_maps(table_name=UNION_PRICE, source_id=101, catalog_source_id=101)
  -> 검색 목록 / 카드 상세 대표 row

card_product.id = 501, union_price_id = 101
  -> product_search_maps(table_name=CARD_PRODUCT, source_id=501, catalog_source_id=101)
  -> 판매 옵션 / 장바구니 / 결제 / 주문 / 재고 차감 row
```

이제 컴퓨터가 `in_stock`을 “헷갈리는” 구조가 아니라, 각 row가 자기 참조 대상 기준으로만 재고 상태를 갖게 됩니다. 대표 row의 `in_stock`은 연결된 카드 판매 row들의 aggregate이고, 판매 row의 `in_stock`은 해당 판매 row 자체의 재고입니다.
