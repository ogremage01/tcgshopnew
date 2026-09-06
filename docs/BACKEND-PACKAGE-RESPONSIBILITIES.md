# Backend Package Responsibilities

백엔드 패키지 구조의 책임과 경계를 빠르게 파악하기 위한 문서입니다.  
기준 경로는 `backend/src/main/java/com/shop`이며, 신규 기능 추가 시 패키지 배치 기준으로도 사용합니다.

---

## 1) 기본 원칙

- 레이어드 아키텍처를 유지합니다. (`Controller -> Service -> Repository`)
- Controller는 요청/응답 처리와 입력 검증 중심으로 유지합니다.
- 비즈니스 규칙, 트랜잭션 경계는 Service 계층에서 처리합니다.
- Repository는 영속성 접근 책임만 가지며, 상위 모듈 DTO를 직접 참조하지 않습니다.
- 여러 도메인을 조합하는 읽기 중심 시나리오는 `application`/`facade` 계층에 둡니다.

---

## 2) 전체 패키지 맵 (`com.shop`)

- `admin`: 관리자 기능(상품/회원/주문/운영성 메타데이터)
- `auth`: 인증/인가 및 토큰 수명주기
- `card`: 카드 가격/메타 원천 데이터 도메인
- `common`: 공통 설정/예외/유틸/인프라 보조
- `config`: 사이트 설정성 도메인(현재 배너)
- `log`: 사용자/주문/동기화 로그 도메인
- `mail`: 메일 발송 및 메일 도메인 저장소
- `mainPage`: 메인 화면용 집계 API
- `order`: 주문 핵심 엔티티/DTO
- `product`: 상품 핵심 도메인(카드/용품/메타/조회)
- `scheduler`: 동기화 배치/스케줄 오케스트레이션
- `search`: 검색 인덱스 및 검색 서비스
- `security`: JWT/레이트리밋 등 보안 필터/인터셉터
- `user`: 회원 도메인

---

## 3) 패키지별 책임 상세

## 3.1 `admin`

### `admin.product`
- 책임: 관리자 상품 등록/수정/삭제, 세트 기준 조회, 메타 동기화/카탈로그 관리 API 제공
- 하위 패키지:
  - `controller`: 관리자 상품 API 진입점
  - `controller.metadata`: 메타 데이터 동기화/조회 API
  - `service`: 관리자 상품 관리 유스케이스
  - `service.metadata`: 관리자 메타 유스케이스
  - `dto.card`: 관리자 상품 요청/응답 DTO
- 대표 클래스:
  - `com.shop.admin.product.controller.AdminSingleProductController`
  - `com.shop.admin.product.controller.metadata.AdminMetadataCatalogController`
  - `com.shop.admin.product.service.AdminProductManagementService`

### `admin.user`
- 책임: 관리자 회원 조회/검색/상태/메모/포인트 관리 API
- 하위 패키지:
  - `controller`
  - `dto`: 관리자 회원 관리 요청/응답 DTO
- 대표 클래스:
  - `com.shop.admin.user.controller.AdminUserController`
  - `com.shop.admin.user.dto.UserManagementRequestDto`

### `admin.order`
- 책임: 관리자 주문 관리 API 엔트리(확장 예정 영역)
- 하위 패키지:
  - `controller`
- 대표 클래스:
  - `com.shop.admin.order.controller.AdminOrderController`

---

## 3.2 `auth`

- 책임: 로그인/회원가입/로그아웃/토큰 재발급과 인증 흐름 관리
- 하위 패키지:
  - `controller`: 인증 API
  - `service`: 인증 도메인 로직
  - `dto`: 인증 요청/응답 DTO
  - `refresh`: 리프레시 토큰 엔티티/리포지토리/서비스/스케줄러
  - `exception`: 인증 예외
- 대표 클래스:
  - `com.shop.auth.controller.AuthController`
  - `com.shop.auth.service.AuthServiceImpl`
  - `com.shop.auth.refresh.entity.RefreshToken`

---

## 3.3 `card`

- 책임: 외부 원천 카드 가격/메타 정보 저장 및 조회
- 하위 패키지:
  - `entity`: 원천 가격 엔티티(Fab/Mtg/TcgP/Union 등)
  - `repository`: 원천 가격 저장소
  - `service`: 원천 가격 조회 유스케이스
  - `metadata`: 게임별 메타(세트/라인/타입) DTO/Entity/Repository/Service
  - `dto`: 카드 원천 데이터 전달 DTO
- 대표 클래스:
  - `com.shop.card.entity.UnionPrice`
  - `com.shop.card.repository.UnionPriceRepository`
  - `com.shop.card.metadata.repository.MtgSetInfoRepository`

---

## 3.4 `common`

- 책임: 전 도메인 공통 인프라 기능 제공
- 하위 패키지:
  - `config`: 보안, CORS, 트랜잭션, MVC 설정
  - `exception`: 전역 예외 처리 및 공통 예외 타입
  - `excel`: 엑셀 업로드/다운로드 공통 처리
  - `fileUpload`: 파일 업로드 추상화
  - `jpa`: JPA 변환기 등 영속성 공통
  - `util`: 암호화/유틸리티
- 대표 클래스:
  - `com.shop.common.config.SecurityConfig`
  - `com.shop.common.exception.GlobalExceptionHandler`
  - `com.shop.common.excel.service.ExcelServiceImpl`

---

## 3.5 `config`

### `config.banner`
- 책임: 메인/사이트 배너 관리(조회/등록/정렬/삭제)
- 하위 패키지:
  - `controller`, `service`, `repository`, `entity`, `dto`
- 대표 클래스:
  - `com.shop.config.banner.service.BannerServiceImpl`
  - `com.shop.config.banner.repository.BannerRepository`

---

## 3.6 `log`

- 책임: 사용자/주문/동기화 로그 저장 및 조회
- 하위 패키지:
  - `user`: 사용자 로그
  - `order`: 주문 로그
  - `sync`: 배치/동기화 로그 + 이벤트
  - `mail`: 메일 관련 로그(확장 여지)
- 대표 클래스:
  - `com.shop.log.sync.service.SyncLogServiceImpl`
  - `com.shop.log.order.entity.OrderLog`
  - `com.shop.log.sync.event.SyncLogEvent`

---

## 3.7 `mail`

- 책임: 인증/알림 메일 발송과 메일 이력 저장
- 하위 패키지:
  - `config`, `service`, `repository`, `entity`, `exception`
- 대표 클래스:
  - `com.shop.mail.service.MailServiceImpl`
  - `com.shop.mail.repository.MailRepository`

---

## 3.8 `mainPage`

- 책임: 메인 페이지 전용 집계 API(배너, 헤더 네비 세트 묶음)
- 하위 패키지:
  - `controller`: 메인 페이지 API
  - `application`: 다중 도메인 조합용 facade
  - `dto`: 메인 응답 DTO
- 대표 클래스:
  - `com.shop.mainPage.controller.MainPageController`
  - `com.shop.mainPage.application.MainHeaderNavFacadeImpl`
  - `com.shop.mainPage.dto.HeaderNavGameSetsDto`

---

## 3.9 `order`

- 책임: 주문 도메인 핵심 모델(주문 정보, 주문 상품, 설정) 정의
- 하위 패키지:
  - `entity`
  - `dto`
- 대표 클래스:
  - `com.shop.order.entity.OrderInfo`
  - `com.shop.order.entity.OrderProduct`
  - `com.shop.order.dto.OrderDto`

---

## 3.10 `product`

- 책임: 상품 도메인의 중심 계층(조회/상세/판매가/메타 연계/재고 반영)
- 하위 패키지:
  - `controller`: 사용자 상품 조회 API
  - `service`: 상품 유스케이스
    - `pricing`: 가격 정책 계산
    - `supply`: 용품(비카드) 관리
  - `repository`: 상품 영속성
    - `card`, `supply`
  - `entity`: 카드/용품/정책 엔티티
  - `dto`: 목록/상세/관리/슬림 응답 모델
  - `metadata`: 세트/제품타입/라인명 메타 관리
  - `mapper`: DTO 매핑 보조
- 대표 클래스:
  - `com.shop.product.controller.ProductController`
  - `com.shop.product.service.CardProductServiceImpl`
  - `com.shop.product.repository.card.CardProductRepositoryImpl`
  - `com.shop.product.metadata.service.TcgPSetNameService`

---

## 3.11 `scheduler`

- 책임: 메타/가격/재고/이미지 동기화 작업 오케스트레이션
- 하위 패키지:
  - `core`: 스케줄러 공통 진입/메타
    - `core.task`: 실제 스케줄 트리거
    - `core.service`: 등록 작업 목록 조회
    - `core.dto`, `core.annotation`, `core.controller`
  - `metadata`: 메타데이터 동기화
    - `metadata.application`: 메타 동기화 유스케이스
    - `metadata.dto`: 동기화 대상/결과 DTO
  - `price`: 가격 동기화
    - `price.application`: 가격 동기화 오케스트레이션
    - `price.source`: 외부 소스별 수집/파싱/연계
    - `price.service`: 배치 저장/링크/유틸 등 도메인 내부 처리
    - `price.dto`: 가격 수집 원시/중간 DTO
  - `image`: 이미지 동기화
    - `image.application`: 이미지 다운로드 유스케이스
    - `image.dto`: 다운로드 대상 DTO
  - `stock`: 재고 동기화
    - `stock.application`: 재고 반영 유스케이스
    - `stock.task`: 스케줄 트리거
- 대표 클래스:
  - `com.shop.scheduler.core.task.CardDataUpdateScheduler`
  - `com.shop.scheduler.metadata.application.MetadataServiceImpl`
  - `com.shop.scheduler.price.application.PriceServiceImpl`

---

## 3.12 `search`

- 책임: 검색 인덱스(`ProductSearchMap`) 유지 및 검색 API/필터링 제공
- 하위 패키지:
  - `service`: 검색/인덱스 동기화 유스케이스
  - `repository`: 검색 인덱스 조회/업서트
  - `entity`: 검색 인덱스 모델
  - `dto`: 검색 조건/결과 모델
  - `event`, `listener`: 상품 변경 이벤트 기반 검색맵 갱신
- 대표 클래스:
  - `com.shop.search.service.ProductSearchMapServiceImpl`
  - `com.shop.search.listener.ProductSearchMapEventListener`
  - `com.shop.search.entity.ProductSearchMap`

---

## 3.13 `security`

- 책임: 인증 토큰 파싱/검증, 요청 레벨 보안 보조 처리
- 하위 구성:
  - JWT 유틸/필터
  - 인증 레이트리밋 인터셉터
- 대표 클래스:
  - `com.shop.security.JwtAuthenticationFilter`
  - `com.shop.security.JwtUtil`
  - `com.shop.security.AuthRateLimitInterceptor`

---

## 3.14 `user`

- 책임: 사용자 조회/수정/탈퇴 등 사용자 도메인 유스케이스
- 하위 패키지:
  - `controller`
  - `service.user`
  - `repository`
  - `entity`
  - `dto.user`
  - `exception`
- 대표 클래스:
  - `com.shop.user.controller.UserController`
  - `com.shop.user.service.user.UserServiceImpl`
  - `com.shop.user.repository.UserRepository`

---

## 4) 패키지 배치 체크리스트 (신규 기능 추가 시)

- HTTP 스펙이 생기면 `controller`에 배치하고, 비즈니스 규칙은 `service`로 이동했는가?
- 데이터 조회/저장은 `repository`로 한정했는가?
- 조회 조합(aggregation)이 여러 도메인을 가로지르면 `application/facade`를 우선 검토했는가?
- DTO는 소유 도메인 패키지에 두고, 중복 정의를 피했는가?
- 패키지명은 소문자 규칙을 지켰는가? (예외/이행 중 패키지는 정리 계획 기록)

---

## 5) 유지보수 메모

- 현재 코드에는 `mainPage`처럼 대문자가 포함된 패키지가 존재합니다.
- Java 패키지 소문자 원칙으로 점진 정리 시, import 경로와 API 스펙/문서 참조를 함께 점검합니다.
- 이 문서는 구조 책임 안내 문서이며, 상세 API 계약은 각 Controller/DTO 및 별도 API 문서를 기준으로 합니다.

---

## 6) 관련 문서

- 패키지 규칙/의존성/네이밍 기준은 `docs/BACKEND-PACKAGE-GUIDELINES.md`를 기준으로 확인합니다.
