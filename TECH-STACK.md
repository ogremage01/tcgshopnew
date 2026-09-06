# 기술 스택 (Technology Stack)

프로젝트에서 현재 사용 중인 기술과, 도입을 추천하는 라이브러리를 정리한 문서입니다.

---

## 1. 현재 사용 중인 기술 스택

### Frontend (`frontend/`)

| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **프레임워크** | Next.js | 16.x | App Router, SSR/SSG, API Rewrite |
| **UI 라이브러리** | React | 19.x | 컴포넌트 기반 UI |
| **언어** | TypeScript | 5.6.x | 정적 타입 |
| **스타일** | Tailwind CSS | 3.4.x | 유틸리티 기반 CSS (darkMode: class) |
| **HTTP 클라이언트** | Axios | 1.13.x | API 호출 |
| **상태 관리** | Zustand | 5.x | 전역/클라이언트 상태 |
| **서버 상태** | TanStack React Query | 5.x | 서버 데이터 캐싱·동기화 |
| **다국어** | i18next, react-i18next, i18next-browser-languagedetector | 25.x / 16.x | i18n |
| **UI 컴포넌트 기반** | Radix UI (Label, Separator) | 2.x / 1.x | 접근성 있는 헤드리스 UI |
| **아이콘** | Lucide React | 0.454.x | 아이콘 |
| **폼·검증** | React Hook Form, Zod, @hookform/resolvers | 7.x / 4.x / 5.x | 폼 상태·스키마 검증 (zodResolver 연동, 로그인 등) |
| **유틸** | class-variance-authority (cva), clsx, tailwind-merge | - | 조건부 클래스·스타일 조합 |
| **빌드/개발** | PostCSS, Autoprefixer | 8.x / 10.x | CSS 처리 |
| **린트** | ESLint, eslint-config-next | 9.x / 16.x | 코드 품질 |

- **출력**: `output: "standalone"` (Next.js 독립 실행 빌드)
- **경로 별칭**: `@/*` → `./src/*`
- **API 연동**: `NEXT_PUBLIC_API_URL` 미설정 시 `/api/*` → `http://localhost:1567/api/*` Rewrite

---

### Backend (`backend/`)

| 구분 | 기술 | 버전 | 용도 |
|------|------|------|------|
| **프레임워크** | Spring Boot | 4.0.3 | 웹·보안·JPA·검증 |
| **언어** | Java | 17 | JDK |
| **빌드** | Gradle (Kotlin DSL) | 8.14 | build.gradle.kts |
| **웹** | spring-boot-starter-web | - | REST API |
| **보안** | spring-boot-starter-security | - | 인증·인가 |
| **ORM** | spring-boot-starter-data-jpa | - | JPA·Hibernate |
| **검증** | spring-boot-starter-validation | - | Bean Validation |
| **인증 토큰** | JJWT (jjwt-api, jjwt-impl, jjwt-jackson) | 0.13.0 | JWT 발급·검증 |
| **DB (운영)** | MariaDB JDBC | - | MariaDB 연결 |
| **DB (개발)** | H2 | - | 인메모리 DB (dev 프로필) |
| **편의** | Lombok | - | 보일러플레이트 감소 |
| **테스트** | spring-boot-starter-*-test (webmvc, security, jackson2) | - | JUnit 5, MockMvc, @MockitoBean 등 |

- **설정**: `application.yml` + `application-dev.yml` (프로필별)
- **기본 API 포트**: 1567

---

## 2. 추천 라이브러리·기술

아래는 현재 구조를 기준으로 추가 도입을 추천하는 항목입니다. 필요에 따라 선택해 사용하면 됩니다.

### Frontend

| 추천 라이브러리 | 용도 | 비고 |
|-----------------|------|------|
| **Sonner** 또는 **React Hot Toast** | 토스트 알림 (성공/에러 메시지) | 가벼운 알림 UI |
| **next-intl** | Next.js App Router용 i18n | 현재 i18next 대안; App Router와 통합 용이 |
| **@tanstack/react-query-devtools** | React Query 디버깅 (캐시·refetch 확인) | devDependencies, 개발 시에만 사용 |
| **MSW (Mock Service Worker)** | API 목업, 프론트 단위/통합 테스트 | 백엔드 없이 UI 개발·테스트 |
| **Vitest** + **Testing Library** | 컴포넌트·훅 단위 테스트 | Jest 대비 Vite/Next와 궁합 좋음 |
| **Playwright** 또는 **Cypress** | E2E 테스트 | 결제·로그인 플로우 등 시나리오 테스트 |
| **shadcn/ui** (추가 컴포넌트) | Button, Input, Dialog, Dropdown 등 | 이미 Radix 기반이므로 확장 용이 |
| **date-fns** 또는 **day.js** | 날짜 포맷·계산 (주문일, 프로모션 기간 등) | 가벼운 날짜 유틸 |

### Backend

| 추천 라이브러리 | 용도 | 비고 |
|-----------------|------|------|
| **Spring Data Redis** | 검색/상품 캐시, 세션 저장소 | README의 Redis 검색 캐시 예시에 활용 |
| **Springdoc OpenAPI (Swagger)** | API 문서 자동 생성 (Swagger UI) | `springdoc-openapi-starter-webmvc-ui` |
| **Flyway** 또는 **Liquibase** | DB 마이그레이션 버전 관리 | 스키마 변경 이력 관리 |
| **Testcontainers** | MariaDB 등 실제 DB로 통합 테스트 | H2와 달리 실제 DB 동작 검증 |
| **MapStruct** | DTO ↔ Entity 매핑 | Lombok과 함께 사용 가능 |
| **Spring Boot Actuator** | 헬스체크, 메트릭 (배포·모니터링) | `/actuator/health` 등 |
| **Micrometer + Prometheus** | 메트릭 수집 (선택) | 운영 환경 모니터링 |

### 공통·인프라

| 추천 | 용도 |
|------|------|
| **Docker / Docker Compose** | 프론트·백엔드·DB·Redis 한 번에 실행 (README 참고) |
| **Nginx** | 리버스 프록시, 정적 파일, 배포 시 프론트/API 라우팅 |
| **GitHub Actions / GitLab CI** | 빌드·테스트·린트 자동화 |

---

## 3. 요약

- **Frontend**: Next.js 16 + React 19 + TypeScript + Tailwind + React Query + Zustand + React Hook Form + Zod + i18n + Radix 기반 UI.
- **Backend**: Spring Boot 4 + Java 17 + JPA + Security + JWT + MariaDB/H2.
- **추가 시 우선 추천**: 토스트 알림, API 문서(Springdoc), Redis 캐시, 테스트(단위/E2E), Docker/Compose. (폼은 React Hook Form + Zod 도입 완료)

**유지보수·추후 개선** (의존성 업데이트, CVE 대응, 주기 점검): **[docs/MAINTENANCE.md](docs/MAINTENANCE.md)** 참고.

문서 갱신 시점: `package.json`, `build.gradle.kts` 기준.
