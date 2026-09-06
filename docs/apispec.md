# API 명세서

> **Base URL**: `http://localhost:8080`  
> **인증 방식**: JWT (Access Token — HttpOnly Cookie `access-token`, Refresh Token — HttpOnly Cookie `refresh-token`)  
> **Content-Type**: `application/json` (파일 업로드는 `multipart/form-data`)

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [회원 (User)](#2-회원-user)
3. [포인트 로그 (User Point Log)](#3-포인트-로그-user-point-log)
4. [장바구니 (Cart)](#4-장바구니-cart)
5. [결제/체크아웃 (Checkout)](#5-결제체크아웃-checkout)
6. [주문 - 회원 (User Order)](#6-주문---회원-user-order)
7. [주문 - 비회원 (Guest Order)](#7-주문---비회원-guest-order)
8. [메인 페이지 (Main Page)](#8-메인-페이지-main-page)
9. [상품 (Product)](#9-상품-product)
10. [게임 공통 (Game)](#10-게임-공통-game)
11. [게임별 배너 (MTG / FAB / LORC / RIFT / SWU)](#11-게임별-배너)
12. [관리자 - 주문 (Admin Order)](#12-관리자---주문-admin-order)
13. [관리자 - 회원 (Admin User)](#13-관리자---회원-admin-user)
14. [관리자 - 사이트 설정 (Admin Site Config)](#14-관리자---사이트-설정-admin-site-config)
15. [관리자 - 봉입 상품 (Admin Sealed Product)](#15-관리자---봉입-상품-admin-sealed-product)
16. [관리자 - 단카드 상품 (Admin Single Product)](#16-관리자---단카드-상품-admin-single-product)
17. [관리자 - 서플라이 상품 (Admin Supply Product)](#17-관리자---서플라이-상품-admin-supply-product)
18. [관리자 - 직접 등록 상품 (Admin Manual Product)](#18-관리자---직접-등록-상품-admin-manual-product)
19. [관리자 - 적립 규칙 (Admin Reward Rule)](#19-관리자---적립-규칙-admin-reward-rule)
20. [관리자 - 메타데이터 (Admin Metadata)](#20-관리자---메타데이터-admin-metadata)

---

## 공통 응답

### 에러 응답

```json
{
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE",
  "errorMessage": "상세 에러 메시지"
}
```

### 페이지네이션 응답 (`PageResponseDto`)

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "pageNumber": 0,
  "pageSize": 10
}
```

---

## 1. 인증 (Auth)

**Base**: `/api/auth`

---

### POST `/api/auth/login`

로그인. 성공 시 `access-token`, `refresh-token` 쿠키가 설정됩니다. 게스트 장바구니가 있으면 자동 병합됩니다.

**Request Body**

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response `200`**

```json
{
  "token": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "user": {
    "publicId": "01HXXX...",
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "point": 1000
  }
}
```

**Response `401`** — 이메일/비밀번호 불일치

---

### POST `/api/auth/register`

회원가입.

**Request Body**

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "password123",
  "passwordConfirm": "password123"
}
```

**Response `200`**

```json
{ "message": "회원가입이 완료되었습니다." }
```

---

### POST `/api/auth/refresh`

Access Token 갱신.

**Cookie** `refresh-token` 필요

**Response `200`**

```json
{
  "token": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "user": { ... }
}
```

---

### POST `/api/auth/logout`

로그아웃. 쿠키가 삭제됩니다.

**Cookie** `refresh-token` 필요

**Response `200`**

```json
{ "message": "로그아웃 되었습니다." }
```

---

## 2. 회원 (User)

**Base**: `/api/user`  
**인증**: `ADMIN` 또는 `USER` 역할 필요

---

### GET `/api/user/me`

내 정보 조회.

**Response `200`**

```json
{
  "id": 1,
  "publicId": "01HXXX...",
  "name": "홍길동",
  "email": "user@example.com",
  "role": "USER",
  "point": 1500,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-06-01T00:00:00"
}
```

---

### GET `/api/user/{publicId}`

특정 회원 정보 조회 (본인만 가능).

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `publicId` | String | 회원 Public ID (ULID) |

**Response `200`** — `UserResponseDto` (위와 동일)

---

### POST `/api/user/change-name`

이름 변경.

**Request Body**

```json
{ "name": "새이름" }
```

**Response `200`**

```json
{ "message": "이름이 변경되었습니다." }
```

---

### POST `/api/user/change-password`

비밀번호 변경.

**Request Body**

```json
{
  "currentPassword": "old_password",
  "newPassword": "new_password",
  "newPasswordConfirm": "new_password"
}
```

**Response `200`**

```json
{ "message": "비밀번호가 변경되었습니다." }
```

---

### POST `/api/user/withdraw`

회원 탈퇴.

**Response `200`**

```json
{ "message": "회원 탈퇴가 완료되었습니다." }
```

---

## 3. 포인트 로그 (User Point Log)

**Base**: `/api/user/point-log`  
**인증**: 로그인 필요

---

### GET `/api/user/point-log`

내 포인트 변동 내역 조회.

**Query Parameter** (Spring Pageable)

| 이름 | 기본값 | 설명 |
|------|--------|------|
| `page` | 0 | 페이지 번호 |
| `size` | 20 | 페이지 크기 |
| `sort` | `actionDate,desc` | 정렬 |

**Response `200`** — `Page<PointLogUserDto>`

```json
{
  "content": [
    {
      "id": 1,
      "changedPoint": 500,
      "beforePoint": 1000,
      "afterPoint": 1500,
      "changeReason": "주문 적립",
      "executorDisplay": "시스템",
      "actionDate": "2024-06-01T10:00:00",
      "isSuccess": true
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "pageNumber": 0,
  "pageSize": 20
}
```

---

## 4. 장바구니 (Cart)

**Base**: `/api/cart`  
**인증**: 비회원(게스트 쿠키) / 회원(JWT) 겸용

---

### GET `/api/cart/me`

장바구니 조회.

**Response `200`**

```json
{
  "id": 1,
  "userId": null,
  "guestId": "guest-uuid",
  "cartItems": [
    {
      "id": 10,
      "searchMapId": 1001,
      "productType": "CARD",
      "currentVisibleStock": 5,
      "price": 3000,
      "priceUsd": 2.5,
      "quantity": 2,
      "imageUrl": "https://...",
      "productNameEn": "Lightning Bolt",
      "productNameKo": "번개 화살",
      "cardProduct": { ... },
      "suppliesProduct": null,
      "sealedProduct": null
    }
  ],
  "createdAt": "2024-06-01T00:00:00",
  "updatedAt": "2024-06-01T00:00:00"
}
```

담긴 상품이 삭제·비노출 처리돼 더 이상 조회되지 않으면 해당 줄을 장바구니에서 제거하고 응답에서도 제외한다(조회 전체가 실패하지 않는다).

---

### POST `/api/cart/items/{searchMapId}`

장바구니 아이템 추가.

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `searchMapId` | Long | 상품 검색맵 ID |

**Request Body**

```json
{ "quantity": 1 }
```

**Response `200`** — `CartDto` (장바구니 전체 반환)

---

### PATCH `/api/cart/items/{searchMapId}`

장바구니 아이템 수량 변경.

**Request Body**

```json
{ "quantity": 3 }
```

**Response `200`** — `CartDto`

**에러 (POST·PATCH 공통)**

| status | message | 설명 |
|--------|---------|------|
| `400` | `STOCK_OVERFLOW` | 요청 수량이 노출 재고 초과 |
| `400` | `PRODUCT_UNAVAILABLE` | 상품이 삭제·비노출 상태. 장바구니에 남아 있던 줄은 제거됨 |

---

### DELETE `/api/cart/items/{searchMapId}`

장바구니 특정 아이템 삭제.

**Response `200`** — `CartDto`

---

### DELETE `/api/cart/items`

장바구니 전체 비우기.

**Response `200`** — `CartDto`

---

## 5. 결제/체크아웃 (Checkout)

**Base**: `/api/checkout`  
**인증**: 비회원 / 회원 겸용

---

### POST `/api/checkout/drafts`

체크아웃 초안 생성. 장바구니를 기반으로 초안을 만듭니다.

**Response `200`**

```json
{ "publicId": "draft-public-id" }
```

---

### GET `/api/checkout/drafts/{publicId}`

체크아웃 초안 조회.

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `publicId` | String | 초안 Public ID |

**Response `200`**

```json
{
  "publicId": "draft-public-id",
  "status": "PENDING",
  "deliveryMethod": "DELIVERY",
  "recipientName": "홍길동",
  "recipientPhone": "010-1234-5678",
  "recipientAddress": "서울시 강남구 ...",
  "subtotalAmount": 30000,
  "deliveryFee": 3000,
  "usedPointAmount": 0,
  "totalAmount": 33000,
  "hasEventTicket": false,
  "items": [
    {
      "searchMapId": 1001,
      "productType": "CARD",
      "productNameEn": "Lightning Bolt",
      "productNameKo": "번개 화살",
      "imageUrl": "https://...",
      "snapshotUnitPrice": 3000,
      "quantity": 2,
      "snapshotTotalPrice": 6000
    }
  ]
}
```

---

### PATCH `/api/checkout/drafts/{publicId}`

체크아웃 초안 수정 (수령인 정보, 결제 수단, 포인트 사용 등).

**Request Body**

```json
{
  "deliveryMethod": "DELIVERY",
  "recipientName": "홍길동",
  "recipientPhone": "010-1234-5678",
  "recipientAddress": "서울시 강남구 ...",
  "recipientAddressDetail": "101동 201호",
  "recipientZipCode": "06000",
  "usedPointAmount": 1000,
  "paymentCurrency": "KRW",
  "paymentCurrencyRate": 1.0,
  "settleKrwAmount": 32000
}
```

**Response `200`** — `CheckoutDraftResponse`

---

### POST `/api/checkout/drafts/{publicId}/validate`

결제 전 유효성 검사.

**Response `200`** — 검증 통과  
**Response `400`** — 재고 부족 등 실패 항목 포함

---

### POST `/api/checkout/drafts/{publicId}/confirm`

매장 직접결제(현금/카드 단말기) 확정.

**Response `200`**

```json
{
  "orderId": 1,
  "paymentStatus": "PAID",
  "confirmedAt": "2024-06-01T10:00:00",
  "guestVerificationCode": null
}
```

---

### POST `/api/checkout/toss/confirm`

토스페이먼츠 결제 확정.

**Request Body**

```json
{
  "paymentKey": "toss-payment-key",
  "orderId": "draft-public-id",
  "amount": 33000
}
```

**Response `200`** — `CheckoutConfirmResponse`  
**Response `4xx`** — `CheckoutConfirmFailureResponse`

```json
{
  "code": "STOCK_INSUFFICIENT",
  "message": "재고가 부족합니다.",
  "failedItems": [...]
}
```

---

### GET `/api/checkout/config`

주문 설정 조회 (배송비, 무료배송 기준액 등).

**Response `200`**

```json
[
  { "configKey": "SHIPPING_FEE", "configValue": "3000", "isEnabled": true },
  { "configKey": "FREE_SHIPPING_THRESHOLD", "configValue": "50000", "isEnabled": true }
]
```

---

## 6. 주문 - 회원 (User Order)

**Base**: `/api/user/orders`  
**인증**: 로그인 필요

---

### GET `/api/user/orders`

내 주문 목록 (페이지네이션).

**Query Parameter** (Spring Pageable)

**Response `200`** — `Page<OrderSimpleDto>`

```json
{
  "content": [
    {
      "id": 1,
      "orderDate": "2024-06-01T10:00:00",
      "orderStatus": "ORDER_RECEIVED",
      "orderTotal": 33000,
      "paymentCurrency": "KRW"
    }
  ],
  ...
}
```

---

### GET `/api/user/orders/latest`

최근 주문 목록 (소량).

**Response `200`** — `List<OrderSimpleDto>`

---

### GET `/api/user/orders/detail/{id}`

주문 상세 조회.

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 주문 ID |

**Response `200`** — `UserOrderDetailDto`

```json
{
  "orderInfo": {
    "id": 1,
    "guest": false,
    "userId": 10,
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "recipientAddress": "서울시 ...",
    "recipientAddressDetail": "101동 201호",
    "postalCode": "06000",
    "orderStatus": "ORDER_RECEIVED",
    "paymentStatus": "PAID",
    "subtotalAmount": 30000,
    "deliveryFee": 3000,
    "usedPointAmount": 0,
    "totalPaymentAmount": 33000,
    "paymentCurrency": "KRW",
    "deliveryTrackingNumber": null,
    "deliveryMemo": null
  },
  "orderCardProductGroups": [...],
  "orderManualProducts": [...],
  "orderSealedProducts": [...]
}
```

---

## 7. 주문 - 비회원 (Guest Order)

**Base**: `/api/guest/orders`

---

### POST `/api/guest/orders/lookup`

비회원 주문 조회.

**Request Body**

```json
{
  "orderId": 1,
  "verificationCode": "GUEST-VERIFICATION-CODE"
}
```

**Response `200`** — `UserOrderDetailDto`

---

## 8. 메인 페이지 (Main Page)

**Base**: `/api/main`

---

### GET `/api/main/banners`

메인 배너 목록.

**Response `200`**

```json
[
  {
    "id": 1,
    "target": "MAIN",
    "imageUrl": "https://...",
    "link": "/products",
    "title": "신규 상품 입고",
    "displayOrder": 1
  }
]
```

---

### GET `/api/main/header-nav/sets`

헤더 네비게이션용 게임별 세트 목록.

**Query Parameter**

| 이름 | 기본값 | 설명 |
|------|--------|------|
| `limit` | 6 | 게임당 최대 세트 수 |

**Response `200`**

```json
[
  {
    "gameCode": "MTG",
    "sets": [
      { "setCode": "MH3", "setName": "Modern Horizons 3", "urlName": "modern-horizons-3" }
    ]
  }
]
```

---

### GET `/api/main/content-blocks`

메인 페이지 콘텐츠 블록 목록.

**Response `200`**

```json
[
  {
    "id": 1,
    "name": "신규 입고",
    "content": "<p>...</p>",
    "imageUrl": "https://...",
    "link": "/products",
    "displayOrder": 1
  }
]
```

---

## 9. 상품 (Product)

**Base**: `/api/products`

---

### GET `/api/products`

상품 목록 검색 (페이지네이션).

**Query Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `keyword` | String | 검색어 |
| `games` | String[] | 게임 필터 (MTG, FAB, LORC 등) |
| `productTypes` | String[] | 상품 유형 필터 |
| `suppliesTypes` | String[] | 서플라이 유형 필터 |
| `manualCategories` | String[] | 수동 등록 카테고리 필터 |
| `rarities` | String[] | 레어도 필터 |
| `setNames` | String[] | 세트명 필터 |
| `setCode` | String | 세트 코드 |
| `searchMode` | String | 검색 모드 |
| `entryState` | String | 입고 상태 |
| `isFoil` | Boolean | 포일 여부 |
| `isInStock` | Boolean | 재고 있음 여부 |
| `page` | 0 | 페이지 번호 |
| `size` | 20 | 페이지 크기 |
| `sort` | | 정렬 |

**Response `200`** — `Page<ProductItemDto>`

```json
{
  "content": [
    {
      "productType": "CARD",
      "productNameEn": "Lightning Bolt",
      "productNameKo": "번개 화살",
      "imageUrl": "https://...",
      "price": 3000,
      "setName": "Magic 2010",
      "setCode": "M10",
      "currentVisibleStock": 5,
      "rewardPercentage": 3.0,
      "card": { ... },
      "sealedProductInfoDto": null,
      "manualProductInfoDto": null
    }
  ],
  ...
}
```

---

### GET `/api/products/search`

상품 텍스트 검색.

**Query Parameter** — `q` (검색어) + 위 `ProductSearchingDto` 파라미터 + Pageable

**Response `200`** — `Page<ProductItemDto>`

---

### GET `/api/products/suggest`

검색어 자동완성.

**Query Parameter**

| 이름 | 기본값 | 설명 |
|------|--------|------|
| `q` | (필수) | 검색어 |
| `limit` | 10 | 결과 수 |

**Response `200`**

```json
{
  "items": [
    { "id": "01HXXX...", "productName": "Lightning Bolt" }
  ]
}
```

---

### GET `/api/products/search/init`

검색 초기 데이터 (상품 목록 + 필터 목록).

**Query Parameter** — `q` + `ProductSearchingDto` + Pageable

**Response `200`**

```json
{
  "products": { ... },
  "games": ["MTG", "FAB"],
  "productTypes": ["CARD", "SEALED"],
  "suppliesTypes": [...],
  "manualCategories": [...],
  "printTypes": [...],
  "rarities": [...],
  "setNames": [...]
}
```

---

### GET `/api/products/search/game/{game}/{setCode}`

특정 게임/세트 상품 목록.

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `game` | String | 게임 코드 (mtg, fab, lorc 등) |
| `setCode` | String | 세트 코드 |

**Response `200`** — `Page<ProductItemDto>`

---

### GET `/api/products/{id}/detail`

상품 상세 조회.

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `id` | String | 상품 Public ID |

**Response `200`** — `ProductItemDto`

---

## 10. 게임 공통 (Game)

**Base**: `/api/game`

---

### GET `/api/game/sets/{productLineId}`

제품 라인의 세트 목록.

**Path Parameter**

| 이름 | 타입 | 설명 |
|------|------|------|
| `productLineId` | Long | 제품 라인 ID |

**Response `200`** — `List<TcgPSetInfoDto>`

```json
[
  { "setCode": "MH3", "setName": "Modern Horizons 3", "urlName": "modern-horizons-3", "releaseDate": "2024-06-07" }
]
```

---

### GET `/api/game/{game}/sets/{setName}`

특정 게임의 세트 정보.

**Response `200`** — `TcgPSetInfoDto`

---

### GET `/api/game/{game}/sets/{setName}/banner`

세트 배너 이미지.

**Response `200`** — `SetBannerDto`  
**Response `404`** — 배너 없음

```json
{
  "game": "MTG",
  "bannerId": 1,
  "imageUrl": "https://...",
  "link": "/products/search/game/mtg/MH3",
  "title": "Modern Horizons 3",
  "active": true
}
```

---

## 11. 게임별 배너

### MTG — `/api/mtg`

| HTTP | 경로 | 반환 |
|------|------|------|
| GET | `/api/mtg/banners` | `List<BannerDto>` |
| GET | `/api/mtg/sets` | `List<MtgSetInfoDto>` |
| GET | `/api/mtg/sets/{setCode}` | `MtgSetInfoDto` |
| GET | `/api/mtg/sets/{setCode}/banner` | `SetBannerDto` |

**MtgSetInfoDto**

```json
{ "setCode": "M10", "name": "Magic 2010", "nameK": "매직 2010", "type": "core", "releaseDate": "2009-07-17" }
```

---

### FAB — `/api/fab`

| HTTP | 경로 | 반환 |
|------|------|------|
| GET | `/api/fab/banners` | `List<BannerDto>` |
| GET | `/api/fab/sets` | `List<FabSetInfoDto>` |
| GET | `/api/fab/sets/{setCode}` | `FabSetInfoDto` |
| GET | `/api/fab/sets/{setCode}/banner` | `SetBannerDto` |

**FabSetInfoDto**

```json
{ "setCode": "WTR", "name": "Welcome to Rathe", "porder": 1 }
```

---

### LORC — `/api/lorc`

| HTTP | 경로 | 반환 |
|------|------|------|
| GET | `/api/lorc/banners` | `List<BannerDto>` |

---

### RIFT — `/api/rift`

| HTTP | 경로 | 반환 |
|------|------|------|
| GET | `/api/rift/banners` | `List<BannerDto>` |

---

### SWU (Star Wars: Unlimited) — `/api/swu`

| HTTP | 경로 | 반환 |
|------|------|------|
| GET | `/api/swu/banners` | `List<BannerDto>` |

---

## 12. 관리자 - 주문 (Admin Order)

**Base**: `/api/admin/orders`  
**인증**: `ADMIN` 역할 필요

---

### POST `/api/admin/orders/list`

주문 목록 조회 (페이지네이션 + 필터).

**Request Body**

```json
{
  "pageParam": { "page": 0, "size": 20 },
  "startDate": "2024-06-01",
  "endDate": "2024-06-30",
  "orderStatusList": ["ORDER_RECEIVED", "PREPARING"]
}
```

**Response `200`** — `Page<AdminOrderSimpleDto>`

```json
{
  "content": [
    {
      "id": 1,
      "orderDate": "2024-06-01T10:00:00",
      "customerName": "홍길동",
      "customerContact": "010-1234-5678",
      "customerEmail": "user@example.com",
      "orderStatus": "ORDER_RECEIVED",
      "totalPaymentAmount": 33000,
      "totalQuantity": 3,
      "orderLineCount": 2,
      "deliveryCompany": null,
      "guest": false,
      "paymentMethod": "카드"
    }
  ],
  ...
}
```

---

### POST `/api/admin/orders/search`

주문 번호(id) 또는 고객명(`recipientName`) 키워드 검색. 기간·상태 필터는 적용하지 않는다.

**Request Body** — `AdminOrderSearchRequest`

```json
{
  "pageParam": { "page": 0, "size": 12, "sort": ["id,desc"] },
  "keyword": "홍길동"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `pageParam` | `PageParam` | O | 페이지네이션 |
| `keyword` | string | O | trim 후 필수. 숫자면 주문 `id` 정확 일치 또는 `recipientName` 부분 일치(OR). 숫자가 아니면 `recipientName` 부분 일치만. |

**Response `200`** — `Page<AdminOrderSimpleDto>` (목록 API와 동일)

**Response `400`** — `keyword`가 비어 있음 (`KEYWORD_REQUIRED`)

---

### GET `/api/admin/orders/today-summary`

오늘 주문 요약.

**Response `200`**

```json
{
  "totalOrderCount": 25,
  "orderPendingCount": 10,
  "orderCompletedCount": 12,
  "orderReceivedCount": 3,
  "totalOrderAmount": 825000
}
```

---

### GET `/api/admin/orders/detail/{id}`

주문 상세 조회.

**Response `200`** — `AdminOrderDetailDto`

```json
{
  "orderInfo": {
    "id": 1,
    "orderDate": "2024-06-01T10:00:00",
    "customerName": "홍길동",
    "orderStatus": "ORDER_RECEIVED",
    "paymentStatus": "PAID",
    ...
  },
  "orderCardProductGroups": [...],
  "orderManualProducts": [...],
  "orderSealedProducts": [...]
}
```

---

### PUT `/api/admin/orders/{id}/status`

주문 상태 변경.

**Request Body**

```json
{ "orderStatus": "PREPARING" }
```

**Response `200`**

---

### PUT `/api/admin/orders/{id}/cancel`

주문 취소.

**Request Body**

```json
{
  "restoreStock": true,
  "restoreCart": false,
  "cancelReason": "고객 변심"
}
```

- `cancelReason` 필수 (Toss `cancelReason`으로 전달)

**Response `200`** — `AdminOrderAdjustmentResponse`

---

### PUT `/api/admin/orders/{id}/products`

주문 상품 부분 수정(수량 감소·라인 삭제). 재계산된 실결제 차액을 Toss `cancelAmount`로 부분 환불.

**Request Body**

```json
{
  "cancelReason": "상품 일부 품절",
  "items": [
    {
      "orderProductId": 101,
      "newQuantity": 1,
      "deleted": false,
      "restoreStock": true
    }
  ]
}
```

- `cancelReason` 필수
- Toss PG 환불 실패 시 `409 TOSS_REFUND_FAILED` (주문 변경 롤백)

**Response `200`** — `AdminOrderAdjustmentResponse`

---

### PUT `/api/admin/orders/{id}/delivery-tracking`

배송 추적 번호 등록.

**Request Body**

```json
{ "deliveryTrackingNumber": "1234567890123" }
```

**Response `200`**

---

### PUT `/api/admin/orders/{id}/delivery-memo`

배송 메모 수정.

**Request Body**

```json
{ "deliveryMemo": "문 앞에 놓아주세요." }
```

**Response `200`**

---

### GET `/api/admin/orders/config`

주문 설정 목록 조회.

**Response `200`** — `List<OrderConfigDto>`

---

### POST `/api/admin/orders/shipping-fee`

배송비 설정 저장.

**Request Body**

```json
{ "configKey": "SHIPPING_FEE", "configValue": "3000", "isEnabled": true }
```

**Response `200`** — `OrderConfigDto`

---

### POST `/api/admin/orders/free-shipping-threshold`

무료배송 기준액 설정 저장.

**Request Body**

```json
{ "configKey": "FREE_SHIPPING_THRESHOLD", "configValue": "50000", "isEnabled": true }
```

**Response `200`** — `OrderConfigDto`

---

## 13. 관리자 - 회원 (Admin User)

**Base**: `/api/admin/user`  
**인증**: `ADMIN` 역할 필요

---

### GET `/api/admin/user/list`

회원 목록 (페이지네이션).

**Query Parameter** — Pageable

**Response `200`** — `PageResponseDto<UserManagementResponseDto>`

```json
{
  "content": [
    {
      "id": 1,
      "name": "홍길동",
      "email": "user@example.com",
      "role": "USER",
      "point": 1500,
      "userStatus": "ACTIVE",
      "userMemo": "",
      "createdAt": "2024-01-01T00:00:00"
    }
  ],
  ...
}
```

---

### GET `/api/admin/user/search`

회원 검색.

**Query Parameter**

| 이름 | 설명 |
|------|------|
| `keyword` | 이름 또는 이메일 |

**Response `200`** — `PageResponseDto<UserManagementResponseDto>`

---

### PUT `/api/admin/user/detail`

회원 정보 수정.

**Request Body**

```json
{
  "id": 1,
  "name": "홍길동",
  "role": "USER",
  "userStatus": "ACTIVE",
  "userMemo": "VIP 고객",
  "password": "",
  "passwordConfirm": ""
}
```

**Response `200`**

---

### PUT `/api/admin/user/point`

회원 포인트 변경.

**Request Body**

```json
{
  "id": 1,
  "point": 500,
  "changeReason": "이벤트 지급"
}
```

**Response `200`** — 변경 후 포인트 (`Long`)

---

### PUT `/api/admin/user/memo`

회원 메모 수정.

**Request Body**

```json
{ "id": 1, "userMemo": "VIP 고객" }
```

**Response `200`**

---

### DELETE `/api/admin/user/{id}`

회원 삭제.

**Response `200`**

---

### GET `/api/admin/user/point-log/recent`

최근 포인트 로그 (시스템 제외).

**Response `200`** — `List<PointLogDto>`

---

### GET `/api/admin/user/point-log/recent/include-system`

최근 포인트 로그 (시스템 포함).

**Response `200`** — `List<PointLogDto>`

---

### GET `/api/admin/user/point-log`

포인트 로그 전체 조회 (페이지네이션).

**Query Parameter**

| 이름 | 설명 |
|------|------|
| `keyword` | 회원명 검색어 |
| `includeSystem` | 시스템 로그 포함 여부 |

**Response `200`** — `PageResponseDto<PointLogDto>`

---

## 14. 관리자 - 사이트 설정 (Admin Site Config)

**Base**: `/api/admin/site-setting`  
**인증**: `ADMIN` 역할 필요

---

### 배너 관리

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/site-setting/banners/list/{target}` | 배너 목록 조회 (`target`: MAIN, MTG 등) |
| POST | `/api/admin/site-setting/banners/add/{target}` | 배너 추가 (multipart/form-data) |
| PUT | `/api/admin/site-setting/banners/order/{target}` | 배너 순서 변경 |
| DELETE | `/api/admin/site-setting/banners/{id}` | 배너 삭제 |

**POST 배너 추가 (form-data)**

| 필드 | 타입 | 설명 |
|------|------|------|
| `imageFile` | File | 배너 이미지 |
| `link` | String | 클릭 링크 |
| `title` | String | 배너 타이틀 |

**PUT 순서 변경 Body**

```json
{ "orderedIds": [3, 1, 2] }
```

---

### 세트 배너 관리

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/site-setting/set-banners/list` | 세트 배너 목록 |
| PUT | `/api/admin/site-setting/set-banners/{game}/{bannerId}` | 세트 배너 수정 (multipart/form-data) |

**PUT form-data 필드**: `imageFile`, `link`, `title`, `active`

---

### 메인 페이지 콘텐츠 관리

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/site-setting/main-page-content/list` | 콘텐츠 목록 |
| POST | `/api/admin/site-setting/main-page-content/add` | 콘텐츠 추가 |
| PUT | `/api/admin/site-setting/main-page-content/{id}` | 콘텐츠 수정 |
| PUT | `/api/admin/site-setting/main-page-content/order` | 콘텐츠 순서 변경 |
| DELETE | `/api/admin/site-setting/main-page-content/{id}` | 콘텐츠 삭제 |

**POST/PUT Body**

```json
{ "name": "신규 입고", "content": "<p>...</p>", "link": "/products" }
```

---

### 에디터 이미지 업로드

**POST `/api/admin/site-setting/upload/main-image`** (multipart/form-data)

| 필드 | 타입 | 설명 |
|------|------|------|
| `image` | File | 이미지 파일 |

**Response `200`** — EditorJS 형식

```json
{ "success": 1, "file": { "url": "https://..." } }
```

---

## 15. 관리자 - 봉입 상품 (Admin Sealed Product)

**Base**: `/api/admin/product/sealed-products`  
**인증**: `ADMIN` 역할 필요

---

### POST `/api/admin/product/sealed-products`

봉입 상품 등록 (multipart/form-data).

| 필드 | 타입 | 설명 |
|------|------|------|
| `productNameEn` | String | 영문명 |
| `productNameKo` | String | 한글명 |
| `game` | String | 게임 코드 |
| `setName` | String | 세트명 |
| `setCode` | String | 세트 코드 |
| `price` | Integer | 가격 |
| `stock` | Integer | 재고 |
| `imageFile` | File | 이미지 |
| `language` | String | 언어 코드 |

---

### GET `/api/admin/product/sealed-products`

봉입 상품 목록.

**Query Parameter**

| 이름 | 설명 |
|------|------|
| `keyword` | 검색어 |
| `game` | 게임 필터 |
| `setCode` | 세트 코드 필터 |
| `language` | 언어 필터 |

**Response `200`** — `Page<SealedProductDto>`

---

### GET `/api/admin/product/sealed-products/facets`

봉입 상품 게임 패싯.

**Response `200`** — `List<SealedProductGameFacetDto>`

---

### PUT `/api/admin/product/sealed-products/{id}`

봉입 상품 수정.

**Request Body** — `SealedProductDto`

---

### DELETE `/api/admin/product/sealed-products/{id}`

봉입 상품 삭제.

---

## 16. 관리자 - 단카드 상품 (Admin Single Product)

**Base**: `/api/admin/product/single-products`  
**인증**: `ADMIN` 역할 필요

---

### GET `/api/admin/product/single-products/search/cards`

카드 Union Price 검색.

**Query Parameter**: `keyword`, `game`, `setCode`, Pageable

**Response `200`** — `UnionPriceAdminSearchResponseDto`

---

### GET `/api/admin/product/single-products/search/products`

등록된 카드 상품 검색.

**Response `200`** — `CardProductAdminSearchResponseDto`

---

### GET `/api/admin/product/single-products/{id}`

특정 카드 상품 관리 목록.

**Path Parameter**: `id` (productId)

**Response `200`** — `Page<CardProductManagementResponseDto>`

---

### PATCH `/api/admin/product/single-products/{id}`

카드 상품 부분 수정.

**Request Body**

```json
{
  "isVisible": true,
  "isPriceLinked": false,
  "storageId": 1,
  "stock": 5,
  "pricingRate": 1.0,
  "price": 3000,
  "memo": "상태 양호"
}
```

**Response `200`** — `CardProductManagementResponseDto`

---

### POST `/api/admin/product/single-products/card`

카드 상품 등록.

**Request Body** — `CardProductRegister`

```json
{
  "productType": "CARD",
  "cardName": "Lightning Bolt",
  "condition": "NM",
  "printType": "NORMAL",
  "language": "EN",
  "stock": 5,
  "price": 3000,
  "storageId": 1,
  "unionPriceId": 101
}
```

---

### POST `/api/admin/product/single-products/delete/{id}`

카드 상품 삭제.

---

### POST `/api/admin/product/single-products/multiple/upload`

카드 상품 엑셀 업로드 (multipart/form-data).

**Response `200`** — `ExcelUploadResultDto`

```json
{
  "successCount": 50,
  "failCount": 2,
  "updatedCount": 10,
  "saved": [...],
  "errors": [...]
}
```

---

### POST `/api/admin/product/single-products/multiple/download`

카드 상품 엑셀 다운로드.

**Request Body** — `ExcelRequestDto`

```json
{
  "gameName": "MTG",
  "setName": "Modern Horizons 3",
  "setCode": "MH3",
  "printType": "NORMAL",
  "condition": "NM",
  "language": "EN",
  "storageId": 1,
  "isVisible": true,
  "maxVisibleStock": 99
}
```

**Response `200`** — `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (Excel 파일)

---

### POST `/api/admin/product/single-products/sync/union-price-search-map`

Union Price 검색맵 동기화.

---

### POST `/api/admin/product/single-products/sync/product-search-map-reference-axis`

상품 검색맵 참조 축 재구성.

---

### GET `/api/admin/product/single-products/search/by-set`

세트별 카드 상품 조회.

**Query Parameter** — `SearchBySetDto`

| 이름 | 설명 |
|------|------|
| `game` | 게임 코드 |
| `set` | 세트 코드 |
| `printTypeFilter` | 프린트 타입 필터 |
| `storageId` | 창고 ID |

**Response `200`** — `List<CardProductManagementResponseDto>`

---

## 17. 관리자 - 서플라이 상품 (Admin Supply Product)

**Base**: `/api/admin/product/supply-products`  
**인증**: `ADMIN` 역할 필요

---

### 서플라이 상품 CRUD

| HTTP | 경로 | 설명 |
|------|------|------|
| POST | `/api/admin/product/supply-products` | 서플라이 등록 (multipart/form-data) |
| GET | `/api/admin/product/supply-products` | 목록 (`Page<SupplyDto>`) |
| GET | `/api/admin/product/supply-products/{id}` | 상세 |
| PUT | `/api/admin/product/supply-products/{id}` | 수정 |
| POST | `/api/admin/product/supply-products/delete/{id}` | 삭제 |

---

### 제조사(Maker) CRUD

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/product/supply-products/makers` | 제조사 목록 |
| POST | `/api/admin/product/supply-products/makers` | 제조사 등록 |
| PUT | `/api/admin/product/supply-products/makers/{id}` | 제조사 수정 |
| POST | `/api/admin/product/supply-products/makers/delete/{id}` | 제조사 삭제 |
| GET | `/api/admin/product/supply-products/makers-by-page` | 제조사 목록 (페이지네이션) |

---

### 서플라이 타입(SupplyType) CRUD

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/product/supply-products/supply-types` | 타입 목록 |
| POST | `/api/admin/product/supply-products/supply-types` | 타입 등록 |
| PUT | `/api/admin/product/supply-products/supply-types/{id}` | 타입 수정 |
| POST | `/api/admin/product/supply-products/supply-types/delete/{id}` | 타입 삭제 |
| GET | `/api/admin/product/supply-products/supply-types-by-page` | 타입 목록 (페이지네이션) |

---

## 18. 관리자 - 직접 등록 상품 (Admin Manual Product)

**Base**: `/api/admin/product/manual-products`  
**인증**: `ADMIN` 역할 필요

| HTTP | 경로 | 설명 |
|------|------|------|
| POST | `/api/admin/product/manual-products` | 상품 등록 (multipart/form-data) |
| GET | `/api/admin/product/manual-products` | 목록 (`Page<ManualProductDto>`) |
| GET | `/api/admin/product/manual-products/{id}` | 상세 |
| PUT | `/api/admin/product/manual-products/{id}` | 수정 |
| DELETE | `/api/admin/product/manual-products/{id}` | 삭제 |

**ManualProductDto**

```json
{
  "id": 1,
  "publicId": "01HXXX...",
  "nameEn": "Playmat",
  "nameKo": "플레이매트",
  "description": "고급 플레이매트",
  "price": 15000,
  "stock": 20,
  "productType": "PLAYMAT",
  "productIp": "MTG",
  "imgUrl": "https://...",
  "isVisible": true
}
```

---

## 19. 관리자 - 적립 규칙 (Admin Reward Rule)

**Base**: `/api/admin/reward-rules`  
**인증**: `ADMIN` 역할 필요

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/reward-rules` | 적립 규칙 목록 |
| POST | `/api/admin/reward-rules` | 규칙 등록 |
| PUT | `/api/admin/reward-rules/{id}` | 규칙 수정 |
| DELETE | `/api/admin/reward-rules/{id}` | 규칙 삭제 |
| PUT | `/api/admin/reward-rules/reorder` | 규칙 순서 변경 |

**RewardRuleDto**

```json
{
  "id": 1,
  "name": "일반 적립",
  "rank": 1,
  "rewardPercentage": 3.0,
  "calRule": "PERCENTAGE",
  "isActive": true,
  "startAt": "2024-01-01T00:00:00",
  "endAt": null
}
```

**PUT Reorder Body**

```json
{ "orderedIds": [2, 1, 3] }
```

---

## 20. 관리자 - 메타데이터 (Admin Metadata)

**Base**: `/api/admin/product/metadata`  
**인증**: `ADMIN` 역할 필요

---

### 메타데이터 동기화

| HTTP | 경로 | 설명 |
|------|------|------|
| POST | `/api/admin/product/metadata/sync` | 전체 메타데이터 동기화 |
| POST | `/api/admin/product/metadata/tcg-p-prices/sync` | TCG Player 가격 동기화 |
| GET | `/api/admin/product/metadata/sync/log` | 동기화 로그 (`Page<SyncLogDto>`) |
| GET | `/api/admin/product/metadata/sync/last-time` | 마지막 동기화 시간 |
| POST | `/api/admin/product/metadata/sync/openbinder-prices` | Openbinder 가격 동기화 |
| POST | `/api/admin/product/metadata/sync/price-check-code-rebuild` | 가격 체크 코드 재구성 |
| POST | `/api/admin/product/metadata/sync/price-check-code-rebuild-selective` | 선택적 가격 체크 코드 재구성 |
| POST | `/api/admin/product/metadata/sync/price-link-rebuild` | 가격 링크 재구성 |
| POST | `/api/admin/product/metadata/sync/price-full-rebuild` | 가격 전체 재구성 |
| POST | `/api/admin/product/metadata/sync/price-link-rescan-nulls` | Null 가격 링크 재스캔 |
| POST | `/api/admin/product/metadata/sync/price-union-save` | Union Price 저장 |
| POST | `/api/admin/product/metadata/sync/price-union-public-id-backfill` | Union Price Public ID 백필 |
| POST | `/api/admin/product/metadata/sync/stock-charge` | 재고 충전 동기화 |

**MetadataSyncResponse**

```json
{ "status": "COMPLETED" }
```

**LastSyncTimesDto**

```json
{
  "lastPriceSyncTime": "2024-06-01T10:00:00",
  "lastMetadataSyncTime": "2024-06-01T09:00:00",
  "lastOpenBinderSyncTime": "2024-06-01T08:00:00",
  "lastImageDownloadSyncTime": "2024-05-31T22:00:00",
  "lastPriceLinkOverwriteSyncTime": "2024-06-01T07:00:00"
}
```

---

### 카탈로그

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/product/metadata/list` | 제품 라인 목록 (`List<TcgPProductLineDto>`) |
| GET | `/api/admin/product/metadata/list/{productLineId}` | 제품 라인의 세트 목록 |
| GET | `/api/admin/product/metadata/fab/sets` | FAB 세트 목록 |
| GET | `/api/admin/product/metadata/sync/game/list` | 동기화 게임 목록 |

---

### 이미지 다운로드

| HTTP | 경로 | 설명 |
|------|------|------|
| POST | `/api/admin/product/metadata/tcg-p-images/download` | TCG Player 이미지 다운로드 |
| POST | `/api/admin/product/metadata/openbinder-images/download` | Openbinder 이미지 다운로드 |

---

### 설정 부트스트랩

**GET `/api/admin/product/metadata/config/bootstrap`**

관리자 상품 설정 초기 데이터 일괄 조회.

**Response `200`** — `ProductConfigBootstrapDto`

```json
{
  "sync": {
    "games": [...],
    "lastTime": { ... }
  },
  "storage": [
    { "id": 1, "storageName": "메인 창고", "description": "", "isDefault": true }
  ],
  "price": {
    "minimum": { "configKey": "MIN_PRICE", "configValue": "500", "updatedAt": "..." },
    "grade": [
      { "id": 1, "grade": "NM", "percentage": 100 },
      { "id": 2, "grade": "LP", "percentage": 85 }
    ]
  }
}
```

---

### 가격 설정

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/product/metadata/minimum-price` | 최소 가격 조회 |
| POST | `/api/admin/product/metadata/minimum-price` | 최소 가격 저장 |
| GET | `/api/admin/product/metadata/grade-price` | 등급별 가격 정책 목록 |
| POST | `/api/admin/product/metadata/grade-price` | 등급별 가격 정책 저장 |
| GET | `/api/admin/product/metadata/us-currency-rate` | USD 환율 조회 |
| POST | `/api/admin/product/metadata/us-currency-rate` | USD 환율 저장 |

---

### 창고(Storage) 관리

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/admin/product/metadata/storage/list` | 창고 목록 |
| POST | `/api/admin/product/metadata/storage` | 창고 등록 |
| PUT | `/api/admin/product/metadata/storage` | 창고 수정 |
| PUT | `/api/admin/product/metadata/storage/delete` | 창고 삭제 |

---

### 카드 언어 코드

**GET `/api/admin/product/card-product-languages`**

**Response `200`** — `List<CardProductLanguageDto>`

```json
[
  { "code": "EN", "displayName": "English", "displayNameKo": "영어" },
  { "code": "KR", "displayName": "Korean", "displayNameKo": "한국어" }
]
```

---

*최종 업데이트: 2026-06-17*
