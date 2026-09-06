# 사용자 API 문서: 장바구니, 체크아웃, 주문, 사용자

이 문서는 이 저장소의 사용자 대상 API를 정리한 문서입니다.

문서 범위:

- 장바구니
- 체크아웃
- 사용자 프로필
- 사용자 포인트 로그
- 회원 주문 조회
- 비회원 주문 조회

아래 경로는 모두 백엔드 서버 루트 기준 상대 경로입니다.

## 1. 공통 규칙

### 인증 방식

- 회원 API는 Spring Security 인증 사용자 정보를 사용합니다.
- 일부 API는 비회원도 사용할 수 있으며, 이 경우 애플리케이션의 guest 식별 로직을 사용합니다.
- 이 코드베이스에서 비회원 장바구니와 체크아웃은 `HttpServletRequest` 와 `Authentication` 을 함께 사용해 식별합니다.

### 공통 에러 응답 형태

서비스 계층에서 `ResponseStatusException` 이 발생하면 대체로 아래 형태로 응답합니다.

```json
{
  "message": "ERROR_CODE"
}
```

사용자 도메인 예외(`UserException`)는 아래 형태로 응답합니다.

```json
{
  "messageCode": "user.error.someCode"
}
```

체크아웃 확정 충돌은 별도 구조를 사용합니다.

```json
{
  "code": "CHECKOUT_CONFLICT",
  "message": "Human-readable message",
  "failedItems": []
}
```

### 구현상 주의할 점

- 장바구니 추가, 수정, 삭제 API는 선언 타입은 `CartDto` 이지만 실제로는 `200 OK` 와 `null` 본문을 반환합니다.
- 사용자 비밀번호 찾기/재설정 관련 일부 API는 현재 플레이스홀더 수준이며, 고정 성공 메시지만 반환합니다.

## 2. 장바구니 API

컨트롤러: `backend/src/main/java/com/shop/cart/controller/CartController.java`

### `GET /api/cart/me`

현재 사용자의 장바구니를 조회합니다.

- 로그인 사용자면 회원 장바구니를 반환합니다.
- 비회원이면 guest 식별값 기준 장바구니를 반환합니다.

응답 예시:

```json
{
  "id": 1,
  "userId": 10,
  "guestId": null,
  "cartItems": [
    {
      "id": 100,
      "searchMapId": 501,
      "productType": "CARD",
      "currentVisibleStock": 3,
      "price": 1500,
      "priceUsd": 1.25,
      "quantity": 2,
      "cartItemUpdatedAt": "2026-07-14T10:30:00",
      "imageUrl": "https://...",
      "productNameEn": "Lightning Bolt",
      "productNameKo": "Lightning Bolt KO",
      "cardProduct": {
        "game": "MTG",
        "condition": "NM",
        "language": "KO",
        "printType": "NORMAL",
        "setCode": "LEA",
        "setNumber": "150",
        "setName": "Limited Edition Alpha"
      },
      "suppliesProduct": null,
      "sealedProduct": null
    }
  ],
  "createdAt": "2026-07-14T10:00:00",
  "updatedAt": "2026-07-14T10:30:00"
}
```

설명:

- `cartItems[].priceUsd` 는 USD 표시용 가격입니다.
- `cartItems[].cartItemUpdatedAt` 는 체크아웃 draft 생성 후 장바구니 라인이 바뀌었는지 확인할 때 사용됩니다.

### `POST /api/cart/items/{searchMapId}`

현재 장바구니에 상품을 추가합니다.

경로 파라미터:

- `searchMapId`: 상품 검색 맵 식별자

요청 본문:

```json
{
  "quantity": 1
}
```

성공 응답:

- `200 OK`
- 본문은 현재 `null`

구현에서 확인되는 대표 에러:

- `400 BAD_REQUEST` + `{"message":"STOCK_OVERFLOW"}`
- `400 BAD_REQUEST` + `{"message":"PRODUCT_UNAVAILABLE"}`
- `400 BAD_REQUEST` + `{"message":"UNSUPPORTED_PRODUCT_TABLE"}`
- `404 NOT_FOUND` + `{"message":"Product not found"}`

### `PATCH /api/cart/items/{searchMapId}`

기존 장바구니 상품의 수량을 수정합니다.

요청 본문:

```json
{
  "quantity": 3
}
```

성공 응답:

- `200 OK`
- 본문은 현재 `null`

에러 패턴은 장바구니 추가 API와 유사합니다.

### `DELETE /api/cart/items/{searchMapId}`

장바구니에서 특정 상품 한 줄을 삭제합니다.

성공 응답:

- `200 OK`
- 본문은 현재 `null`

### `DELETE /api/cart/items`

현재 장바구니를 비웁니다.

성공 응답:

- `200 OK`
- 본문은 현재 `null`

## 3. 체크아웃 API

컨트롤러: `backend/src/main/java/com/shop/checkout/controller/CheckoutController.java`

관련 enum:

- `DeliveryMethod`: `DELIVERY`, `STORE_PICKUP`
- `DraftStatus`: `READY`, `CONFIRMED`, `CANCELLED`, `EXPIRED`, `REPLACED`

### `POST /api/checkout/drafts`

현재 장바구니를 기준으로 체크아웃 draft 를 생성합니다.

동작 요약:

- 회원/비회원 식별값 기준으로 draft 를 만듭니다.
- 장바구니가 비어 있으면 실패합니다.
- 같은 사용자에게 기존 `READY` draft 가 있으면 대체될 수 있습니다.

성공 응답:

```json
{
  "publicId": "01JZ..."
}
```

대표 에러:

- `400 BAD_REQUEST` + `{"message":"EMPTY_CART"}`
- `400 BAD_REQUEST` + `{"message":"PRODUCT_UNAVAILABLE"}`
- `400 BAD_REQUEST` + `{"message":"OUT_OF_STOCK"}`

### `GET /api/checkout/drafts/{publicId}`

현재 사용자에게 속한 체크아웃 draft 를 조회합니다.

응답 예시:

```json
{
  "publicId": "01JZ...",
  "status": "READY",
  "deliveryMethod": "DELIVERY",
  "recipientName": "Kim",
  "recipientAddress": "Seoul ...",
  "recipientPhone": "010-0000-0000",
  "recipientEmail": "kim@example.com",
  "orderRequest": "Leave at door",
  "subtotalAmount": 15000,
  "deliveryFee": 3000,
  "standardDeliveryFee": 3000,
  "usedPointAmount": 1000,
  "totalAmount": 17000,
  "totalProductAmount": 18000,
  "expiresAt": "2026-07-14T11:00:00",
  "createdAt": "2026-07-14T10:30:00",
  "confirmedOrderId": null,
  "items": [
    {
      "searchMapId": 501,
      "productType": "CARD",
      "productNameEn": "Lightning Bolt",
      "productNameKo": "Lightning Bolt KO",
      "imageUrl": "https://...",
      "imageUrlEn": "https://...",
      "imageUrlKo": "https://...",
      "snapshotUnitPrice": 1500,
      "quantity": 2,
      "snapshotTotalPrice": 3000,
      "snapshotPointAmount": 30
    }
  ],
  "hasEventTicket": false
}
```

설명:

- 이 응답은 주문 직전 화면을 고정하기 위한 snapshot 성격이 강합니다.
- `items[].snapshotUnitPrice` 와 `snapshotTotalPrice` 는 draft 시점 기준 값입니다.
- `confirmedOrderId` 는 이미 확정된 draft 에서만 채워질 수 있습니다.

대표 에러:

- `404 NOT_FOUND` + `{"message":"DRAFT_NOT_FOUND"}`
- `409 CONFLICT` + `{"message":"DRAFT_REPLACED"}`
- `410 GONE` + `{"message":"DRAFT_EXPIRED"}`
- `403 FORBIDDEN` + `{"message":"DRAFT_FORBIDDEN"}`

### `PATCH /api/checkout/drafts/{publicId}`

체크아웃 draft 의 배송/수령 정보와 결제 관련 값을 갱신합니다.

요청 본문:

```json
{
  "deliveryMethod": "DELIVERY",
  "recipientName": "Kim",
  "recipientAddress": "Seoul ...",
  "recipientPhone": "010-0000-0000",
  "recipientEmail": "kim@example.com",
  "orderRequest": "Leave at door",
  "usedPointAmount": 1000,
  "paymentCurrency": "KRW",
  "paymentCurrencyRate": 1,
  "settleKrwAmount": 17000
}
```

설명:

- `usedPointAmount` 는 클라이언트가 보내더라도 서버에서 다시 검증합니다.
- `paymentCurrency` 는 현재 코드상 `KRW`, `USD` 같은 값을 전제로 합니다.
- `settleKrwAmount` 는 정산 기준 KRW 금액입니다.

대표 에러:

- `404 NOT_FOUND` + `{"message":"DRAFT_NOT_FOUND"}`
- `409 CONFLICT` + `{"message":"DRAFT_NOT_PATCHABLE"}`
- `410 GONE` + `{"message":"DRAFT_EXPIRED"}`
- `403 FORBIDDEN` + `{"message":"DRAFT_FORBIDDEN"}`
- `400 BAD_REQUEST` + `{"message":"INSUFFICIENT_POINTS"}`
- `400 BAD_REQUEST` + `{"message":"GUEST_CANNOT_USE_POINTS"}`

### `POST /api/checkout/drafts/{publicId}/confirm`

체크아웃 draft 를 최종 확정하고 주문을 생성합니다.

동작 요약:

- 확정 시점에 가격과 재고를 다시 검증합니다.
- 회원/비회원 소유권을 다시 검사합니다.
- 이미 확정된 draft 에 대해서는 중복 주문 대신 기존 결과를 돌려주도록 설계되어 있습니다.

성공 응답 예시:

```json
{
  "orderId": 123,
  "paymentStatus": "PAYMENT_SKIPPED",
  "confirmedAt": "2026-07-14T10:35:00",
  "guestVerificationCode": "123456"
}
```

설명:

- `guestVerificationCode` 는 비회원 주문에서 의미가 있습니다.
- 현재 구현은 PG 연동이 완결된 상태가 아니어서 `paymentStatus` 가 `PAYMENT_SKIPPED` 로 내려올 수 있습니다.

충돌 응답 예시:

```json
{
  "code": "CHECKOUT_CONFLICT",
  "message": "Some items changed during checkout",
  "failedItems": [
    {
      "searchMapId": 501,
      "productNameKo": "Lightning Bolt KO",
      "reason": "OUT_OF_STOCK",
      "snapshotUnitPrice": 1500,
      "currentUnitPrice": 1500,
      "requestedQuantity": 2,
      "availableStock": 1
    }
  ]
}
```

대표 에러:

- `404 NOT_FOUND` + `{"message":"DRAFT_NOT_FOUND"}`
- `403 FORBIDDEN` + `{"message":"DRAFT_FORBIDDEN"}`
- `409 CONFLICT` + 구조화된 `CheckoutConfirmFailureResponse`

### `GET /api/checkout/config`

체크아웃 화면에서 사용하는 주문 설정 목록을 반환합니다.

응답 예시:

```json
[
  {
    "configKey": "DELIVERY_FEE",
    "configValue": "3000",
    "isEnabled": true
  }
]
```

현재 코드에서 유추 가능한 용도:

- 배송비
- 무료배송 기준 금액
- 주문 관련 설정 토글

## 4. 사용자 API

컨트롤러: `backend/src/main/java/com/shop/user/controller/UserController.java`

클래스 레벨 권한:

- `@PreAuthorize("hasAnyRole('ADMIN', 'USER')")`

즉, 이 컨트롤러의 모든 API는 인증된 회원 권한이 필요합니다.

### `GET /api/user/me`

현재 로그인한 사용자의 프로필을 조회합니다.

응답 예시:

```json
{
  "publicId": "01JZ...",
  "name": "Kim",
  "email": "kim@example.com",
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-07-01T10:00:00",
  "point": 5000,
  "role": "USER"
}
```

설명:

- 내부 숫자 ID 는 컨트롤러에서 `null` 처리 후 반환합니다.

### `POST /api/user/withdraw`

현재 로그인한 사용자를 탈퇴 처리합니다.

성공 응답:

```json
{
  "message": "Withdrawn"
}
```

### `POST /api/user/change-name`

사용자 이름을 변경합니다.

요청 본문:

```json
{
  "name": "New Name"
}
```

성공 응답:

```json
{
  "message": "Name changed"
}
```

대표 에러:

- 이름이 비어 있으면 `400 BAD_REQUEST` + `{"message":"Name is required"}`
- 인증 사용자 해석 실패 시 `401 UNAUTHORIZED` + `{"messageCode":"user.error.unauthorized"}`

### `POST /api/user/change-password`

사용자 비밀번호를 변경합니다.

요청 본문:

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password",
  "passwordConfirm": "new-password"
}
```

설명:

- 서버는 `newPassword` 대신 `password` 도 대체 필드로 허용합니다.
- `passwordConfirm` 을 보내면 새 비밀번호와 일치해야 합니다.

성공 응답:

```json
{
  "message": "Password changed"
}
```

대표 에러:

- `400 BAD_REQUEST` + `{"message":"Current password is required"}`
- `400 BAD_REQUEST` + `{"message":"New password is required"}`
- `400 BAD_REQUEST` + `{"messageCode":"user.error.passwordNotMatch"}`
- `400 BAD_REQUEST` + `{"messageCode":"user.error.invalidPassword"}`
- `404 NOT_FOUND` + `{"messageCode":"user.error.userNotFound"}`

### 현재 플레이스홀더 성격인 비밀번호 관련 API

아래 API 들은 엔드포인트는 존재하지만, 현재 컨트롤러 기준으로는 고정 성공 메시지만 반환합니다.

- `POST /api/user/find-password`
- `POST /api/user/reset-password`
- `POST /api/user/send-email-reset-password-code`
- `POST /api/user/verify-email-reset-password-code`

현재 응답 형태:

```json
{
  "message": "..."
}
```

예시:

- `"Password found"`
- `"Password reset"`
- `"Email reset password code sent"`
- `"Email reset password code verified"`

### `GET /api/user/{publicId}`

경로의 `publicId` 가 현재 인증 사용자와 정확히 일치할 때만 사용자 정보를 조회할 수 있습니다.

성공 응답 형태는 `GET /api/user/me` 와 동일합니다.

대표 에러:

- 다른 사용자의 `publicId` 를 요청하면 `403 FORBIDDEN`
- 사용자가 없으면 `404 NOT_FOUND`

## 5. 사용자 포인트 로그 API

컨트롤러: `backend/src/main/java/com/shop/user/controller/UserPointLogController.java`

### `GET /api/user/point-log`

현재 로그인한 사용자의 포인트 로그를 Spring `Page` 형태로 반환합니다.

쿼리 파라미터:

- `page`
- `size`
- `sort`

응답 예시:

```json
{
  "content": [
    {
      "id": 1,
      "changedPoint": 100,
      "beforePoint": 900,
      "afterPoint": 1000,
      "changeReason": "ORDER_REWARD",
      "executorDisplay": "system",
      "actionDate": "2026-07-14T09:00:00",
      "isSuccess": true
    }
  ],
  "pageable": {},
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

대표 에러:

- 사용자 식별 실패 시 `401 UNAUTHORIZED`

## 6. 회원 주문 API

컨트롤러: `backend/src/main/java/com/shop/order/controller/UserOrderController.java`

### `GET /api/user/orders`

현재 로그인한 회원의 주문 목록을 페이지 형태로 조회합니다.

쿼리 파라미터:

- `page`
- `size`
- `sort`

응답 아이템 예시:

```json
{
  "id": 123,
  "orderDate": "2026-07-14T10:35:00",
  "orderStatus": "PAID",
  "orderTotal": 17000,
  "paymentCurrency": "KRW"
}
```

대표 에러:

- `401 UNAUTHORIZED`

### `GET /api/user/orders/latest`

현재 로그인한 회원의 최신 5건 주문을 조회합니다.

응답:

- `OrderSimpleDto` 배열

### `GET /api/user/orders/detail/{id}`

특정 회원 주문의 상세를 조회합니다.

최상위 응답 구조:

```json
{
  "orderInfo": {},
  "orderCardProductGroups": [],
  "orderManualProducts": [],
  "orderSealedProducts": []
}
```

`orderInfo` 주요 필드:

- `id`
- `guest`
- `userId`
- `recipientName`
- `recipientAddress`
- `recipientPhone`
- `recipientEmail`
- `orderRequest`
- `orderStatus`
- `paymentStatus`
- `deliveryCompany`
- `deliveryTrackingNumber`
- `deliveryMemo`
- `paymentCurrency`
- `totalProductAmount`
- `totalPaymentAmount`
- `usedPointAmount`
- `actualPaymentAmount`
- `paymentDate`
- `deliveryFee`
- `totalQuantity`
- `orderLineCount`
- `paymentMethod`
- `paymentCurrencyRateSnapshot`
- `totalEarnedPoints`

주문 상품 구조:

- `orderCardProductGroups[]` 는 `game` 기준 그룹입니다.
- 카드 상품은 기본 주문 라인 필드에 더해 아래 필드를 가집니다.
  - `game`
  - `setCode`
  - `setName`
  - `releaseDate`
  - `setNumber`
  - `printType`
  - `printing`
  - `language`
  - `condition`
- `orderManualProducts[]` 는 기본 `OrderProductDto` 구조를 사용합니다.
- `orderSealedProducts[]` 는 `game`, `language` 가 추가됩니다.

기본 주문 라인 필드:

- `id`
- `orderInfoId`
- `productId`
- `quantity`
- `price`
- `totalPrice`
- `imageUrl`
- `productNameEn`
- `productNameKo`
- `rewardPoints`

대표 에러:

- `401 UNAUTHORIZED`
- `403 FORBIDDEN` + `{"message":"ORDER_ACCESS_DENIED"}`
- `404 NOT_FOUND` + `{"message":"ORDER_NOT_FOUND"}`

## 7. 비회원 주문 API

컨트롤러: `backend/src/main/java/com/shop/order/controller/GuestOrderController.java`

### `POST /api/guest/orders/lookup`

주문 ID 와 인증 코드를 이용해 비회원 주문을 조회합니다.

요청 본문:

```json
{
  "orderId": 123,
  "verificationCode": "123456"
}
```

성공 응답:

- 회원 주문 상세와 동일한 `UserOrderDetailDto` 구조

대표 에러:

- `400 BAD_REQUEST` + `{"message":"INVALID_REQUEST"}`
- `404 NOT_FOUND` + `{"message":"GUEST_ORDER_NOT_FOUND"}`

조회 조건:

- `orderId` 는 필수입니다.
- `verificationCode` 는 필수입니다.
- 해당 주문이 실제 비회원 주문이어야 합니다.
- 저장된 비회원 인증 코드와 trim 이후 값이 정확히 일치해야 합니다.

## 8. 다음 문서화 추천 범위

다음 순서로 이어서 문서화하면 흐름이 좋습니다.

1. 상품 조회 API
2. 메인 페이지 API
3. 관리자 주문 API
4. 관리자 상품 API

