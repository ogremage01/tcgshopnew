# Backend Package Guidelines

백엔드 패키지 정리 이후, 구조 일관성을 유지하기 위한 기준입니다.

## 1) Repository 의존 규칙

- Repository는 상위 모듈(`admin`, `user`) DTO를 직접 참조하지 않는다.
- Repository 입력/출력 모델은 다음 중 하나만 사용한다.
  - 해당 도메인 Entity
  - 해당 도메인 DTO
  - 공통 슬림 DTO(`common` 또는 별도 중립 패키지)

## 2) DTO 소유권 규칙

- DTO는 데이터를 소유하는 도메인 패키지에 위치한다.
  - 예: TCGP metadata 관련 DTO는 `product.metadata.dto`에 둔다.
- 관리자 전용 API DTO는 `admin.<domain>.dto`에 둔다.
  - 예: 관리자 회원 관리 DTO는 `admin.user.dto`에 둔다.
- 다른 도메인에서 DTO가 필요하면 원 소유 패키지를 import하여 사용하고, 재정의하지 않는다.

## 3) 집계(Composition) 서비스 규칙

- 여러 도메인의 데이터를 조합하는 기능은 도메인 서비스가 아니라 `application`/`facade` 계층에 둔다.
- 집계 계층은 트랜잭션 경계를 최소화하고, 조합/정렬/매핑 책임만 가진다.
- 도메인 고유 규칙(영속성 변경, 상태 전이)은 각 도메인 서비스에서 처리한다.

## 4) Service 인터페이스 예외 기준

- 기본 원칙은 `Service interface + ServiceImpl` 구조를 유지한다.
- 아래 조건을 모두 만족하면 인터페이스 생략 가능:
  - 외부 주입 지점이 1개 이하
  - 도메인 정책을 거의 포함하지 않는 단순 오케스트레이션
  - 테스트 대역(Stub/Mock) 필요성이 낮음
- 예외 적용 시 클래스 상단에 사유 주석을 남긴다.

## 5) Entity -> DTO 변환 책임

- 기본 원칙: Entity는 영속성 모델만 표현하고, DTO 변환은 Mapper/Assembler에서 수행한다.
- 기존 `toDto()`가 이미 널리 사용되는 엔티티는 즉시 제거하지 않고 점진 전환한다.
- 신규 엔티티는 `toDto()`를 추가하지 않는다.

## 6) 네이밍/패키지 규칙

- Java 패키지명은 소문자를 사용한다. (`mainpage`, `listener`)
- 클래스명은 의미 단위 대소문자를 일관되게 유지한다. (`SetName`, `ServiceImpl`)
- 오탈자 패키지는 기능 변경 없이 우선 정리한다.
- `scheduler` 패키지는 역할 기준으로 `core`/`application`/`source`를 우선 사용한다.
  - `core`: 스케줄 트리거/등록 메타
  - `application`: 유스케이스 오케스트레이션
  - `source`: 외부 수집/파싱/연계

## 7) 관련 문서

- 패키지별 책임/경계/대표 구성요소는 `docs/BACKEND-PACKAGE-RESPONSIBILITIES.md`를 기준으로 확인한다.
