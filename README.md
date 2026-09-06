# tcgshopnew — TCG 카드 쇼핑몰 (공개 스냅샷)

MTG, Flesh and Blood, Disney Lorcana, Star Wars Unlimited, Riftbound 등 **TCG 싱글 카드·실링(Sealed)** 을 다루는 온라인 쇼핑몰과 관리자 백오피스입니다.

이 저장소는 **실서비스 코드를 정제한 공개본**입니다. 결제 키, 운영 도메인, 사업자 정보, 고객 데이터, 배포 비밀, 외부 가격·이미지 소스 URL은 넣지 않았습니다. 코드는 열람용이며 상업적 재사용은 `LICENSE`를 따릅니다. 로컬에서는 테스트용 Toss 키와 예시 환경 변수로 실행하세요.

**Next.js 16 (App Router)** + **Spring Boot 4.0.3** 풀스택입니다.

## 대표 설계/개선

- **결제:** Draft snapshot + 재검증 + 멱등 처리로 금액·재고 정합성 보장
- **검색:** 통합 읽기 모델 + 병렬 facet 조회로 대규모 카드 검색 구조 개선
- **운영:** 외부 가격 오류, SSE, JWT refresh race 등 실제 운영 장애 대응

구현 포인트는 `union_prices`(세 소스를 정규화한 내부 기준 테이블), `CheckoutDraft`, `product_search_maps`, 관리자 주문·입고 화면입니다. 막힌 지점과 대응은 아래 **문제 해결 사례**에 있습니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| 백엔드 | Java 17, Spring Boot 4.0.3, JPA, QueryDSL, Gradle 8.14 |
| DB | MariaDB (로컬 `dev` 기본) |
| 캐시 | Redis(선택, 검색 facet) |
| 프론트엔드 | Next.js 16, React 19, TypeScript, Tailwind CSS, shadcn/ui, Zustand, TanStack Query |
| 인증 | JWT(access·refresh), HttpOnly 쿠키, `@PreAuthorize` |
| 인프라 | Docker Compose, Nginx |
| 외부 연동 | Toss Payments(테스트 키), 가격·카탈로그 소스(공개본 스텁), Scryfall 이미지 |

## 저장소 구조

| 경로 | 역할 |
|------|------|
| `backend/` | Spring Boot REST API |
| `frontend/` | Next.js 사용자·관리자 UI (`/{locale}` 사이트, `/admin` 백오피스) |
| `docs/` | 구현·운영 문서, DDL |
| `docker/` | 통합 Dockerfile |
| `deploy/example/` | 배포 예시 (placeholder 경로) |

### 도메인 요약

- **스키마**: `dev`/`demo`는 `ddl-auto: update`, `prod`는 `validate`
- **검색**: `product_search_maps` — 카드·실링·수동상품을 검색·장바구니·체크아웃에 통합
- **체크아웃**: Draft 재검증 → Toss 승인 후 확정, 또는 매장 직접결제 확정. 확정 실패 시 승인 취소 시도

## 실행 방법

### 사전 요구사항

- Node.js 20+
- Java 17+
- MariaDB
- Redis (선택)
- 온라인 결제 실험 시 Toss **테스트** 키

### 백엔드

기본 프로필은 `dev`, 포트는 **18567** (`SERVER_PORT`).

```bash
cd backend
./gradlew bootRun
```

Windows: `gradlew.bat bootRun`

MariaDB에 `shop` DB를 만들고 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`를 맞춥니다.

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

- http://localhost:3000
- `NEXT_PUBLIC_API_URL` 미설정 시 `/api` 등을 `http://127.0.0.1:18567`로 프록시합니다.

### Docker

| 방식 | 명령 | 접속 |
|------|------|------|
| 통합 앱 | `docker compose up -d app` | http://localhost |
| 분리 | `docker compose up -d` | API :1567, 프론트 :3001, Redis :6379 |

Compose의 `SERVER_PORT`는 1567입니다. 컨테이너에서 MariaDB에 붙이려면 `DB_URL`을 넣으세요.

## 환경 변수

예시: `backend/.env.example`, `frontend/.env.example`, `deploy/example/.env.server.example`

시크릿 실값은 커밋하지 마세요. Toss는 `test_gsk_` / `test_gck_` 만 사용합니다.

| 변수 | 설명 |
|------|------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MariaDB |
| `JWT_SECRET` | 32자 이상 |
| `PERSONAL_DATA_ENCRYPTION_KEY` | 개인정보 AES (Base64 32바이트) |
| `TOSS_WIDGET_SECRET_KEY` | Toss 위젯 **테스트** 시크릿 |
| `NEXT_PUBLIC_TOSS_PAYMENTS_CLIENT_KEY` | Toss 위젯 **테스트** 클라이언트 키 |
| `CORS_ORIGINS`, `NEXT_PUBLIC_SITE_URL` | 기본 예시는 `https://tcgshop.example` |
| `REDIS_HOST`, `SEARCH_FACETS_CACHE_ENABLED` | facet 캐시(선택) |

## API 개요

`/api` prefix.

| 영역 | Base path |
|------|-----------|
| 인증 | `/api/auth` |
| 상품·검색 | `/api/products` |
| 장바구니 | `/api/cart` |
| 체크아웃 | `/api/checkout` |
| Toss 승인 | `/api/checkout/toss` |
| 내 주문 | `/api/user/orders` |
| 비회원 주문 | `/api/guest/orders` |
| 관리자 | `/api/admin/**` (`hasRole('ADMIN')`) |

- SSR: fetch
- CSR: axios

## 문제 해결 사례

개발·연동 중에 실제로 막힌 지점입니다. 상세 보고서는 `docs/`에 있습니다.

### 세 가격 소스를 `union_prices` 하나로 묶음

카드 판매가를 정하려면 시장가가 필요한데, 원천이 세 갈래였습니다. TCGPlayer(`tcg_p_prices`), MTG 카탈로그(`mtg_prices`), FAB 카탈로그(`fab_prices`)는 ID·세트 코드·인쇄(foil) 표기가 제각각이라, 검색·판매가·이미지를 요청 시점에 조인하면 규칙이 흩어집니다.

각 소스 행에 `check_code`를 만들고, 사람이 고친 값은 `check_code_refined`로 남긴 뒤 서로 링크합니다. 그다음 한 테이블 `union_prices`로 적재해 **카드 가격·카탈로그의 내부 기준 테이블(canonical source)** 로 씁니다. 원천 데이터는 소스 테이블에 그대로 두고, 서비스가 판매가·검색·이미지를 붙일 때 이 정규화 행을 기준으로 삼습니다. 적재 순서는 TCG → FAB/MTG이며, 같은 코드는 뒤 단계가 덮어씁니다. `check_code_refined`는 unique입니다.

판매 단위(`card_product`)와 검색 읽기 모델(`product_search_maps`)은 이 행을 기준으로 파생됩니다. 등급 비율·환율은 `UnionPrice.price` 위에 올립니다. 공개본에서는 매칭 산식·소스 URL·적재 구현은 스텁입니다.

### 관리자 주문 알림 SSE가 붙지 않음

프론트(`:3000`)에서 API(`:18567`)로 EventSource를 직접 열면 CORS로 `Failed to fetch`가 났고, `next.config` rewrite로 `/api/*`를 통째로 넘기면 스트림이 버퍼링되며 `Failed to proxy` 500이 났습니다.

브라우저는 same-origin만 보고, Next.js Route Handler가 백엔드 스트림을 pipe하도록 바꿨습니다. rewrite 패턴에서 `admin/alarm/subscribe`만 제외합니다. 주문 알림은 트랜잭션 **커밋 후**(`AFTER_COMMIT`)에만 보냅니다.

→ `docs/ADMIN-SSE-ALARM-IMPLEMENTATION-REPORT.md`

### 재고가 있는데 검색에서 품절로 보임

카드 카탈로그 행과 실제 판매 행이 검색맵에서 둘 다 `UNION_PRICE`를 쓸 수 있었습니다. `card_product` 재고는 있는데, 다른 행의 `inStock=false`가 목록에 남는 경우가 있었습니다.

`tableName`을 참조 테이블로 고정했습니다. `UNION_PRICE`는 카탈로그 표시, `CARD_PRODUCT` / `SEALED_PRODUCT` / `MANUAL_PRODUCT`만 장바구니·결제·재고 차감 대상입니다.

→ `docs/PRODUCTSEARCHMAP-REFERENCE-REFACTOR-REPORT.md`

### 검색 첫 로딩이 수 초

`search/init`이 상품 조회와 facet용 DISTINCT를 한 요청에서 순차로 돌렸고, SSR도 init 다음 목록을 이어서 호출했습니다. 키워드 `LIKE`와 집계가 겹치면 체감 지연이 컸습니다.

단계별 시간 로그와 느린 SQL 로그를 넣고, facet을 전용 스레드 풀에서 병렬 조회했습니다. 프론트는 `Promise.all`로 init·목록을 동시에 요청합니다. facet만 Redis에 넣을 수 있게 했고, 기본은 캐시 없이 동작합니다.

→ `docs/SEARCH-PERFORMANCE-IMPLEMENTATION-REPORT.md`

### 외부 시세 0원이 그대로 팔림

가격 소스에서 `UnionPrice.price = 0`이 들어오면 `CardProduct`가 공개 상태로 남을 수 있었습니다.

ingestion 후 자동 숨김·복원과, 등록/수정 시 0원이면 비공개 가드를 넣었습니다. 관리자가 직접 숨긴 상품과 구분하려고 `hiddenByPriceError`를 썼습니다. 자동 복원은 이 플래그가 켜진 행만 대상입니다.

→ `docs/PRICE-ERROR-CARD-IMPLEMENTATION-REPORT.md`

### 토스 연동: 전화번호 거절, 승인 후 확정 실패

폼/DB 값이 `010-1234-5678`처럼 하이픈을 포함하면 토스 SDK가 특수문자 오류를 냈습니다. 위젯에 넘기기 직전에 숫자만 남기도록 정규화했습니다.

승인 후 DB 확정이 실패하면 결제 취소를 시도하고, 같은 draft 재요청은 비관적 잠금과 기존 주문 반환으로 멱등 처리합니다. 결제 timeout 후 자동 대사는 아직 없습니다.

### 토큰 만료 직후 요청이 한꺼번에 401

여러 API가 동시에 401을 받으면 refresh를 각각 호출해 레이스가 났습니다. axios 인터셉터에서 `refreshPromise` 하나를 공유하고, 나머지는 그 결과를 기다리게 했습니다. refresh 자체가 401이면 세션을 지우고 로그인으로 보냅니다.

## 알려진 제한 (이 공개본)

- 실서비스 URL·운영 키·고객 데이터는 포함하지 않습니다.
- 외부 가격·카탈로그·카드 이미지 소스 URL과 요청 헤더는 공개본에서 제외했습니다. 스케줄러 골격은 남아 있고, 실제 fetch는 스텁입니다.
- 가격 매칭(check_code), 소스 링크, union 적재, 판매가·환율 산식은 공개본에서 스텁입니다. 인터페이스와 호출 흐름만 남겼습니다.
- 결제 timeout 후 자동 대사는 없습니다.
- 비밀번호 찾기 API는 스텁입니다. 로그인 후 비밀번호 변경은 동작합니다.
- 저장소 안 GitHub Actions는 없습니다.

상세 구현 노트는 `docs/`를 참고하되, 문서 일부는 공개용으로 도메인·경로만 바꿔 둔 상태입니다.
