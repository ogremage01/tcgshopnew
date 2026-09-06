# 가격 오류 카드 관리 구현 보고서

작성일: 2026-06-18

---

## 1. 목적과 범위

| 구분 | 내용 |
|------|------|
| 목적 | `UnionPrice.price = 0`인 카드 제품이 공개 상태로 판매되는 문제 방지 |
| 포함 | 자동 숨김·자동 복원 스케줄 로직, 등록/수정 시 guard, 관리자 조회 페이지 |
| 제외 | `UNION_PRICE` 행 visibility 변경(카탈로그 모델 유지), 자동 inStock 복원(기존 reconcile 스케줄러에 위임) |

---

## 2. 배경

TCG 카드 시장가 데이터(`UnionPrice.price`)는 외부 소스(TCGPlayer, Open Binder 등)에서 주기적으로 ingestion된다.  
일부 카드의 경우 소스 데이터에 가격이 0으로 들어오는 경우가 있으며, 해당 상태에서 `CardProduct.isVisible = true`이면 고객에게 0원(또는 최저가 미만)으로 판매될 수 있다.

---

## 3. 설계 개요

### 3.1 핵심 플래그: `hiddenByPriceError`

`isVisible = false`인 카드가 **가격 오류로 자동 숨겨진 것**인지 **관리자가 의도적으로 숨긴 것**인지 구분하기 위해 `CardProduct` 엔티티에 `hiddenByPriceError` 필드를 추가했다.

| 상태 | `isVisible` | `hiddenByPriceError` |
|------|-------------|----------------------|
| 가격 오류 자동 숨김 | false | **true** |
| 관리자 수동 숨김 | false | false |
| 정상 공개 | true | false |

자동 복원은 `hiddenByPriceError = true AND price > 0`인 행만 대상으로 하므로, 관리자가 의도적으로 숨긴 상품은 건드리지 않는다.

### 3.2 트리거 시점

| 경로 | 시점 | 처리 |
|------|------|------|
| `UnionPriceIngestionServiceV2Impl` | 가격 ingestion 완료 후 (스케줄러) | `syncPriceErrorVisibility()` — 숨김 + 복원 |
| `CardProductServiceImpl` | 등록·수정 시 저장 직전 | guard — price = 0이면 강제 비공개 |

### 3.3 `syncPriceErrorVisibility()` 실행 순서

```
Step 1 — 숨김
  조건: price=0 AND isVisible=true AND isDeleted=false
  처리: CardProduct → isVisible=false, hiddenByPriceError=true
        ProductSearchMap(CARD_PRODUCT) → isVisible=false, inStock=false

Step 2 — 복원
  조건: hiddenByPriceError=true AND price>0 AND isDeleted=false
  처리: CardProduct → isVisible=true, hiddenByPriceError=false
        ProductSearchMap(CARD_PRODUCT) → isVisible=true, inStock=(currentVisibleStock > 0) [JOIN]
```

### 3.4 `patchCardProduct` guard 분기

| 요청 | price 상태 | 결과 `isVisible` | 결과 `hiddenByPriceError` |
|------|-----------|-----------------|--------------------------|
| `isVisible=true` | price = 0 | false | true |
| `isVisible=true` | price > 0 | true | false |
| `isVisible=false` | 무관 | false | false (관리자 의도 → 자동 복원 제외) |

---

## 4. 구현 내역

### 4.1 DB 스키마 변경

```sql
ALTER TABLE card_product
ADD COLUMN hidden_by_price_error TINYINT(1) NULL DEFAULT NULL
COMMENT '가격 오류(price=0)로 자동 비공개 처리된 경우 1';
```

기존 데이터는 `NULL`로 유지된다. 복원 조건이 `hidden_by_price_error = true`이므로 `NULL` 행은 자동 복원 대상에서 제외된다.

### 4.2 Backend 변경 파일

#### `CardProduct.java`
- `hiddenByPriceError Boolean` 필드 추가 (`@Column(name = "hidden_by_price_error")`)

#### `CardProductRepository.java`
새 쿼리 5개 추가 (모두 native SQL):

| 메서드 | 설명 |
|--------|------|
| `findPriceErrorCards(Pageable)` | 관리자 페이지용 price=0 카드 페이지 조회 |
| `findPublicIdsByZeroPriceAndVisible()` | 숨길 대상 publicId 목록 |
| `findPublicIdsByNonZeroPriceAndHiddenByError()` | 복원 대상 publicId 목록 |
| `bulkHideZeroPriceLinkedProducts()` | 비공개 bulk UPDATE (JOIN) |
| `bulkRestoreNonZeroPriceLinkedProducts()` | 재공개 bulk UPDATE (JOIN) |

#### `ProductSearchMapRepository.java`
새 메서드 2개 추가 (native SQL):

| 메서드 | 설명 |
|--------|------|
| `hideByCardProductPublicIds(List<String>)` | CARD_PRODUCT 행 isVisible=false, inStock=false |
| `restoreByCardProductPublicIds(List<String>)` | CARD_PRODUCT 행 isVisible=true, inStock=card_product.current_visible_stock > 0 (JOIN) |

#### `PriceErrorCardService.java` + `PriceErrorCardServiceImpl.java` (신규)
패키지: `com.shop.admin.product.service`

```java
void syncPriceErrorVisibility();
Page<CardProductManagementResponseDto> getPriceErrorCards(Pageable pageable);
```

`syncPriceErrorVisibility()`는 `@Transactional`로 hide → restore를 원자적으로 처리한다.  
`getPriceErrorCards()`는 `@Transactional(readOnly = true)`로 lazy load를 커버한다.

#### `UnionPriceIngestionServiceV2Impl.java`
`saveAllPricesToUnion()` 및 `saveOpenBinderPricesToUnion()` 끝에 훅 추가:

```java
runUnionIngestionStep("price_error_visibility", priceErrorCardService::syncPriceErrorVisibility);
```

기존 `runUnionIngestionStep` 패턴을 따르므로 step 실패 시 로그만 남기고 나머지 ingestion은 계속된다.

#### `CardProductServiceImpl.java`
`applyPriceErrorGuard(CardProduct entity)` 메서드 추가, 아래 3곳에서 호출:

- `patchCardProduct()` — `applyPatchBody()` 후 guard 실행 (entity에 unionPrice 포함하도록 `findByIdWithUnionPrice()` 사용)
- `registerCardProduct()` — `save()` 전에 guard 실행
- `registerOrUpdateCardProducts()` — `saveAll()` 직전 루프 내 guard 실행 (엑셀 업로드 포함)

#### `AdminSingleProductController.java`
```
GET /api/admin/product/single-products/price-error-cards
파라미터: page(default 0), size(default 20, sort=id DESC)
반환:     Page<CardProductManagementResponseDto>
권한:     ADMIN
```

### 4.3 Frontend 변경 파일

#### `_hooks/usePriceErrorCards.ts` (신규)
- 마운트 시 자동 조회, 페이지 변경 시 재조회
- `AbortController`로 중복 요청 취소
- `removeFromList(id)` — 관리자 삭제 후 로컬 목록 반영

#### `price-error-card/page.tsx`
- 기존 skeleton 교체
- 총 건수(`totalElements`) 헤더 표시
- `AdminCardProductTable` 재사용 (facets 빈 배열, 필터 비활성)

---

## 5. 데이터 흐름

```
[스케줄러] UnionPriceIngestionServiceV2Impl
    ↓ saveAllPricesToUnion() / saveOpenBinderPricesToUnion()
    ↓ runUnionIngestionStep("price_error_visibility", ...)
PriceErrorCardServiceImpl.syncPriceErrorVisibility()
    ├─ Step 1 Hide
    │   ├─ CardProductRepository.findPublicIdsByZeroPriceAndVisible()
    │   ├─ CardProductRepository.bulkHideZeroPriceLinkedProducts()
    │   └─ ProductSearchMapRepository.hideByCardProductPublicIds()
    └─ Step 2 Restore
        ├─ CardProductRepository.findPublicIdsByNonZeroPriceAndHiddenByError()
        ├─ CardProductRepository.bulkRestoreNonZeroPriceLinkedProducts()
        └─ ProductSearchMapRepository.restoreByCardProductPublicIds()

[등록/수정] CardProductServiceImpl
    ├─ registerCardProduct()        → applyPriceErrorGuard()
    ├─ registerOrUpdateCardProducts() → applyPriceErrorGuard() (loop)
    └─ patchCardProduct()           → applyPriceErrorGuard()

[관리 페이지] GET /price-error-cards
    → PriceErrorCardServiceImpl.getPriceErrorCards(Pageable)
    → CardProductRepository.findPriceErrorCards(Pageable)
    → Page<CardProductManagementResponseDto>
```

---

## 6. 제약 및 운영 참고

| 항목 | 내용 |
|------|------|
| `UNION_PRICE` SearchMap | 항상 `isVisible = true` 유지 (카탈로그 모델). 이번 변경에서 건드리지 않음 |
| 복원 후 `inStock` | `restoreByCardProductPublicIds()`가 `card_product`를 JOIN해 `current_visible_stock > 0` 여부를 즉시 반영. `reconcileUnionPriceInStock()`은 `UNION_PRICE` 행만 다루므로 `CARD_PRODUCT` 행은 이 쿼리에서 직접 처리 |
| 대량 `IN (:publicIds)` | 현재 단일 쿼리. 대량 데이터 환경에서는 500건 단위 청크 처리를 고려 |
| 기존 `NULL` 데이터 | 마이그레이션 전 기존 `card_product` 행은 `hidden_by_price_error = NULL`. 복원 조건(`= true`)에 해당하지 않으므로 안전 |
| 관리자 override | 관리자가 `PATCH /{id}` 로 `isVisible=true` 강제 설정 시 `hiddenByPriceError=false` 클리어됨 → 이후 ingestion에서 가격이 다시 0이 되면 재차 자동 숨김 |
