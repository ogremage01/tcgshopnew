##해야할 일

--어드민 보안 관련
1. @EnableMethodSecurity 활성화
2. Admin 컨트롤러 메서드에 적용
3. SecurityConfig에 URL 레벨 방어도 병행 권장

--백엔드 보안 관련 (추가)
1. [완료] 에러 응답 표준화 - 로그인 실패 시 "이메일/비밀번호 오류" 구분 없이 동일 messageCode 반환 (계정 열거 방지)

--백엔드 패키지 정리
1. [완료] `product` 서비스의 `admin` DTO 직접 의존 제거 (`product.dto.card.management` 커맨드/응답 DTO로 분리)
2. [완료] 관리자 회원 관리 DTO를 `user.dto.user` -> `admin.user.dto`로 이관
3. [완료] 패키지 가이드 문서에 관리자 전용 DTO 소유권 규칙 반영 (`admin.<domain>.dto`)

--프론트엔드 보안 관련
1. 401 자동 토큰 갱신 인터셉터 구현 (silent refresh)

--로그 작성

 [] USER_LOGIN_SUCCESS / FAIL
 [] ORDER_CREATED
 [] PAYMENT_SUCCESS / FAIL
 [] ORDER_STATUS_CHANGED
 [] ERROR (전역 예외)
 [] SYNC
 [] POINT

 --성능 최적화

 [] 캐시도입
 [] 인덱스(단일, 복합) 작성
 [] 조회 후 DTO화에서 DTO 조회로 변경
 [] @async로 일부 메서드 비동기화(거의 필요 없음)
 [] N+1 문제 개선(있을 경우)

--환경변수
 [] DB_URL, DB_USERNAME, DB_PASSWORD - db 접속 정보
 [] JWT_SECRET - JWT 서명 키
 [] PERSONAL_DATA_ENCRYPTION_KEY - 개인정보 AES-256 암호화 키
 [] MAIL_USERNAME, MAIL_PASSWORD - 인증 메일용 구글 계정
 [] BCRYPT_STRENGTH - BCrypt 해싱 라운드 수 (기본값 10)
 [] TCG_API_KEY - TCG 가격 동기화 외부 API 키
 [] PG_API_KEY, PG_SECRET - 결제 PG사 연동 키 (결제 기능 구현 시)
 [] REDIS_URL, REDIS_PASSWORD - Redis 접속 정보 (캐시 도입 시)

 --체크해야 될 내용
 [] supply들이 입력되고도 searchmap이 정상적으로 작동하는가
 [] (상동)하고도 개별 제품에서 currentVisibleStock이 정상적으로 보여지는가
 [] FAB 세트 리스트
 [] 달러/원 결제 로직이 지금 엉망임. 수정해야함.

 --회원관련
 []비밀번호 리셋 로직