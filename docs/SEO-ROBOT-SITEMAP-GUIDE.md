### robots.txt / sitemap 작성 가이드

이 문서는 쇼핑몰 프론트엔드(Next.js)에서 검색 엔진용 `robots.txt`와 `sitemap.xml`을 어떻게 관리할지 정리한 가이드이다.

---

### 1. 현재 구조 개요

- **실제 응답 위치**
  - `robots.txt` 응답: `GET /robots.txt`
  - `sitemap.xml` 응답: `GET /sitemap.xml`
- **구현 파일 (단일 소스: 메타 라우트 우선)**
  - `robots` 메타 라우트: `frontend/src/app/(meta)/robots.ts`
  - `sitemap` 메타 라우트: `frontend/src/app/(meta)/sitemap.ts`
  - 헬퍼: `frontend/src/lib/sitemap/*`
  - `frontend/public/robots.txt` — 정적 fallback (메타 라우트와 동일 정책 유지)
- Next App Router 기준으로 메타 라우트에서 **동적으로 robots/sitemap을 생성**한다.

---

### 2. robots.txt 작성 요령

- **목표**
  - 공개 허브·검색·게임 페이지는 크롤링 허용.
  - 관리자·장바구니·인증·마이페이지 등은 크롤링 차단.
  - `sitemap.xml` 위치를 명시한다.

- **Disallow 목록** (`frontend/src/lib/sitemap/robots-disallow.ts`)

| 패턴 | 이유 |
|------|------|
| `/admin` | 관리자 UI |
| `/*/cart` | 장바구니 (로케일 prefix `ko`/`en`) |
| `/*/cart/checkout` | 결제 |
| `/*/login` | 로그인 |
| `/*/register` | 회원가입 |
| `/*/mypage` | 마이페이지 |
| `/*/guest-order-check` | 비회원 주문 조회 |

- **주의사항**
  - `Disallow: /` 를 추가하면 전체 사이트가 비노출될 수 있다.
  - 스테이징 전체 차단이 필요하면 `robots.ts`에서 환경 변수로 분기한다.

---

### 3. sitemap 정책

#### 포함 (로케일 `ko`, `en` 각각)

| path | priority | 비고 |
|------|----------|------|
| `/` | 1.0 | 홈 |
| `/search` | 0.9 | 통합 검색 |
| `/special/products` | 0.8 | 수동상품 목록 |
| `/game/mtg` … `/game/rift` | 0.85 | 게임 허브 |
| `/game/{game}/{set}` | 0.75 | 세트별 상품 목록 (API 동적) |

#### 제외

- 상품 상세 `/products/{id}` — 내부 링크·검색으로 발견 가능, URL 수·갱신 부담을 줄이기 위해 sitemap 미포함.
- `/cart`, `/login`, `/mypage` 등 비공개·저가치 페이지.
- `headerNavConfig`에만 있고 `page.tsx`가 없는 경로 (`/supplies/*`, `/mtg` 등).

#### 게임 세트 API 매핑

| 게임 | API | URL 세그먼트 |
|------|-----|----------------|
| MTG | `GET /api/mtg/sets` | `setCode` → `/game/mtg/{setCode}` |
| FAB | `GET /api/game/sets/62` | `urlName` |
| SWU | `GET /api/game/sets/79` | `urlName` |
| LORC | `GET /api/game/sets/71` | `urlName` |
| RIFT | `GET /api/game/sets/89` | `urlName` |

productLineId 상수: `frontend/src/config/game-product-lines.ts`

#### 캐싱

- `sitemap.ts`: `export const revalidate = 86400` (24시간)
- 세트 API fetch: `next: { revalidate: 86400 }`
- API 장애 시 해당 게임 세트만 생략, sitemap 전체는 500이 나지 않도록 처리.

---

### 4. 환경 변수

- `NEXT_PUBLIC_SITE_URL`: `https://{운영 도메인}` (sitemap/robots 절대 URL)
- `API_SERVER_URL`: sitemap 세트 수집용 백엔드 (Docker: `http://backend:1567`)

---

### 5. 변경 시 체크리스트

- **robots.txt 수정 시**
  - [ ] `Disallow: /` 전체 차단이 없는지 확인.
  - [ ] `robots.ts`, `robots-disallow.ts`, `public/robots.txt` 정책이 일치하는지 확인.

- **sitemap 수정 시**
  - [ ] `NEXT_PUBLIC_SITE_URL` 이 운영 도메인인지 확인.
  - [ ] 신규 공개 `page.tsx` 경로가 `static-routes.ts`에 반영되었는지 확인.
  - [ ] 게임 세트 URL이 실제 링크(`MtgSetList`, `GameSetListItem`)와 일치하는지 확인.

---

### 6. 점검 방법

```bash
curl -s http://localhost:3000/robots.txt
curl -s http://localhost:3000/sitemap.xml | head -40
```

확인 항목:

- URL 형식: `https://{도메인}/{ko|en}/...`
- `/ko/cart`, `/ko/products` 등 없는·비공개 경로가 sitemap에 없는지
- MTG: `/game/mtg/{setCode}`, 기타 TCG: `/game/{game}/{urlName}`

---

### 7. 향후 확장 (상품 상세 sitemap)

필요 시:

1. `GET /api/seo/sitemap-products` — `is_visible=true` + `source_public_id` distinct 목록
2. URL 5만 건 이상이면 Next.js `generateSitemaps`로 분할
