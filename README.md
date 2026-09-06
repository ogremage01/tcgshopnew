# tcgshopnew — TCG 카드 쇼핑몰 (공개 스냅샷)

MTG, Flesh and Blood, Disney Lorcana, Star Wars Unlimited, Riftbound 등 **TCG 싱글 카드·실링(Sealed)** 을 다루는 온라인 쇼핑몰과 관리자 백오피스입니다.

이 저장소는 **실서비스 코드를 정제한 공개본**입니다. 결제 키, 운영 도메인, 사업자 정보, 고객 데이터, 배포 비밀, 외부 가격·이미지 소스 URL은 넣지 않았습니다. 코드는 열람용이며 상업적 재사용은 `LICENSE`를 따릅니다. 로컬에서는 테스트용 Toss 키와 예시 환경 변수로 실행하세요.

**Next.js 16 (App Router)** + **Spring Boot 4.0.3** 풀스택입니다.

## 이 프로젝트에서 볼 것

- 서버 `CheckoutDraft` 스냅샷, 금액 불변식, 비관적 잠금, 조건부 재고 차감
- Toss Payments 승인 후 주문 확정, 실패 시 취소·멱등키 (테스트 키)
- `product_search_maps` 읽기 모델, QueryDSL facet, 선택적 Redis 캐시
- 관리자 주문 수정·환불, 오프라인 입고/판매, 검색·상품 운영 화면

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

## 알려진 제한 (이 공개본)

- 실서비스 URL·운영 키·고객 데이터는 포함하지 않습니다.
- 외부 가격·카탈로그·카드 이미지 소스 URL과 요청 헤더는 공개본에서 제외했습니다. 스케줄러 골격은 남아 있고, 실제 fetch는 스텁입니다.
- 가격 매칭(check_code), 소스 링크, union 적재, 판매가·환율 산식은 공개본에서 스텁입니다. 인터페이스와 호출 흐름만 남겼습니다.
- 결제 timeout 후 자동 대사는 없습니다.
- 비밀번호 찾기 API는 스텁입니다. 로그인 후 비밀번호 변경은 동작합니다.
- 저장소 안 GitHub Actions는 없습니다.

상세 구현 노트는 `docs/`를 참고하되, 문서 일부는 공개용으로 도메인·경로만 바꿔 둔 상태입니다.
