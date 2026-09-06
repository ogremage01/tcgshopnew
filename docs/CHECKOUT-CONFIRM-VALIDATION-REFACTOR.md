# Checkout Confirm 백엔드 자체 검증 강화 리팩토링 보고서

작성일: 2026-05-14

---

## 1. 배경 및 문제 정의

기존 `CheckoutConfirmServiceImpl.confirm()` 로직은 결제 확정 시 아래와 같은 검증만 수행하고 있었다.

- 라인별 단가 (`snapshotUnitPrice`)가 현재 DB 값과 일치하는지
- 상품 가시성/삭제 여부
- 재고 수량 충분 여부

그러나 다음 불변식들은 **confirm 시점에 재검산되지 않아**, 이론적으로 조작된 드래프트 스냅샷이 검출 없이 통과될 수 있었다.

| # | 불변식 | 기존 처리 |
|---|--------|-----------|
| ① | `snapshotTotalPrice == snapshotUnitPrice × quantity` | 미검증 |
| ② | `Σ(snapshotTotalPrice) == draft.subtotalAmount` | 미검증 |
| ③ | `max(0, subtotal + deliveryFee − usedPoints) == totalAmount` | 미검증 |
| ④ | `usedPointAmount ≤ subtotal + deliveryFee` | 미검증 |
| ⑤ | `buildOrderProduct` — 확정 주문 금액을 스냅샷 값 그대로 사용 | DB 재산출 없음 |

---

## 2. 변경 내용

### 2-1. `CheckoutLineHandler` 인터페이스 — `resolveCurrentUnitPrice()` 추가

```
backend/src/main/java/com/shop/checkout/lines/CheckoutLineHandler.java
```

DB/계산 기준 현재 단가를 반환하는 메서드를 인터페이스 계약에 명시했다.

```java
/** validate() 통과 후, 실제 DB/계산 기준 단가를 반환한다 */
BigDecimal resolveCurrentUnitPrice(CheckoutDraftItem line);
```

---

### 2-2. `ManualProductCheckoutLineHandler` 수정

```
backend/src/main/java/com/shop/checkout/lines/ManualProductCheckoutLineHandler.java
```

**`resolveCurrentUnitPrice()` 구현**
- `manualProductRepository.findById(sourceId)` 조회 후 `m.getPrice()`를 반환한다.

**`buildOrderProduct()` 재산출**

이전:
```java
.price(line.getSnapshotUnitPrice())
.totalPrice(line.getSnapshotTotalPrice())
```

이후:
```java
long unitPriceLong = m.getPrice() == null ? 0L : m.getPrice();
BigDecimal confirmedUnit = BigDecimal.valueOf(unitPriceLong).setScale(0, RoundingMode.HALF_UP);
BigDecimal confirmedTotal = confirmedUnit.multiply(BigDecimal.valueOf(line.getQuantity()));
.price(confirmedUnit)
.totalPrice(confirmedTotal)
```

확정 주문 라인 금액이 스냅샷 값이 아닌 **confirm 시점 DB 값 기반으로 재산출**되어 저장된다.

---

### 2-3. `UnionPriceCheckoutLineHandler` 수정

```
backend/src/main/java/com/shop/checkout/lines/UnionPriceCheckoutLineHandler.java
```

**`resolveCurrentUnitPrice()` 구현**
- `cardProductRepository.findByPublicId()` 조회 후 `checkoutShowingPriceCalculator.resolveShowingPrice(cp)`를 반환한다.

**`buildOrderProduct()` 재산출**
- `ManualProductCheckoutLineHandler`와 동일한 방식으로 `confirmedUnit × quantity`를 재산출해 저장한다.

---

### 2-4. `CheckoutConfirmServiceImpl` — `verifyAmountInvariants()` 추가

```
backend/src/main/java/com/shop/checkout/service/CheckoutConfirmServiceImpl.java
```

#### 호출 위치

`validateLineItems()` 통과 직후, 재고 예약(`tryReserveStock`) 이전에 호출한다.

```
validateLineItems(draft)       단가·재고·가시성 검증
        ↓ 통과
verifyAmountInvariants(draft)  ← 신규: 금액 불변식 검산
        ↓ 통과
tryReserveStock(...)           재고 원자 차감
```

#### 검증 항목

```
① 각 라인: snapshotTotalPrice == snapshotUnitPrice × quantity
② 소계:    Σ(snapshotTotalPrice) == draft.subtotalAmount
③ 합계:    max(0, subtotal + deliveryFee - usedPoints) == totalAmount
④ 포인트:  usedPointAmount >= 0
           usedPointAmount <= subtotal + deliveryFee
```

`validateLineItems()`가 이미 단가를 DB와 대조했으므로, 이 시점에서 `snapshotUnitPrice`는 신뢰 가능한 값이다. 따라서 ① 검증이 ② 검증의 전제 조건을 충족한다.

#### 실패 응답

불변식 위반 시 기존 패턴과 동일하게 `409 Conflict`로 응답한다.

```json
{
  "code": "AMOUNT_INTEGRITY_ERROR",
  "message": "<위반 항목별 메시지>",
  "failedItems": []
}
```

---

## 3. 변경 전/후 confirm 흐름 비교

```
[변경 전]
draft 조회 → 소유자 확인 → 상태 확인
→ validateLineItems (단가·재고·가시성)
→ tryReserveStock
→ 포인트 차감
→ OrderInfo/OrderProduct 저장 (스냅샷 값 그대로)

[변경 후]
draft 조회 → 소유자 확인 → 상태 확인
→ validateLineItems (단가·재고·가시성)
→ verifyAmountInvariants (금액 불변식 4가지)   ← 신규
→ tryReserveStock
→ 포인트 차감
→ OrderInfo/OrderProduct 저장 (DB 재산출 값)   ← 신규
```

---

## 4. 부수 효과 및 영향 범위

| 항목 | 내용 |
|------|------|
| DB 추가 쿼리 | 없음. `validateLineItems()`에서 이미 조회한 경로와 동일하며, `buildOrderProduct()`는 기존에도 `findById`를 호출하고 있었음 |
| API 응답 변화 | 정상 플로우(정직한 클라이언트)에서는 응답 없음. 불변식 위반 시 기존에 없던 `AMOUNT_INTEGRITY_ERROR` 409 추가 |
| 프론트 영향 | 정상 요청은 영향 없음 |
| 주문 저장 값 변화 | `OrderProduct.price`, `totalPrice`, `snapshotUnitPrice`가 스냅샷 기준에서 confirm 시점 DB 기준으로 변경됨. `validate()`가 단가 일치를 보장하므로 정상 플로우에서 값은 동일 |

---

## 5. 검증 불변식이 방어하는 시나리오

| 시나리오 | 방어 불변식 |
|----------|-------------|
| `snapshotTotalPrice`를 `snapshotUnitPrice × quantity`보다 낮게 조작 | ① |
| 라인 합계를 맞추되 `draft.subtotalAmount`를 낮게 조작 | ② |
| `usedPointAmount`를 실제 상품 금액 이상으로 조작해 실질 결제액 0으로 만들기 | ④ |
| `totalAmount`를 `subtotal + deliveryFee - usedPoints`보다 낮게 조작 | ③ |