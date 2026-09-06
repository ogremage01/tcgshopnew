# 운영·관리 가이드 (인수인계용)

이 문서는 시스템 관리 책임을 다음 관리자에게 이관할 때 참고하는 **운영·인수인계 가이드**입니다.  
관리자가 손을 떼기 전에 이 문서를 채우고, 새 관리자와 함께 검토하면 됩니다.

---

## 1. 시스템 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | tcgshop (쇼핑몰 풀스택) |
| 구성 | Next.js(프론트) + Spring Boot(백엔드) + H2/MariaDB |
| 배포 형태 | Docker (통합 이미지 `app` 또는 backend/frontend 분리) |

### 실행 방식별 포트

| 방식 | 접속 URL | 비고 |
|------|-----------|------|
| **통합 앱** (권장) | http://localhost (80) | `docker compose up -d app` |
| 백엔드만 | http://localhost:1567 | backend 서비스 |
| 프론트만 | http://localhost:3001 | frontend 서비스 (API는 1567) |
| 로컬 개발 | 백엔드 1567, 프론트 3000 | `./gradlew bootRun`, `npm run dev` |

### 디렉터리/역할

- `backend/` — Spring Boot (Java 17)
- `frontend/` — Next.js (Node 18+)
- `docker/` — 통합 이미지용 Dockerfile, nginx, start.sh
- `docker-compose.yml` — 로컬/테스트용 Docker 구성 (H2 사용)

---

## 2. 인수인계 시 전달할 정보 (체크리스트)

다음 항목을 **비밀번호/토큰은 비공개 채널**로 전달하고, 이 문서에는 “보관 위치”만 적어 두세요.

- [ ] **서버/호스트**
  - SSH 접속 정보 (호스트, 사용자, 키/비밀번호 보관 위치)
  - 서버에 Docker 설치 여부 및 버전
- [ ] **Docker 이미지 저장소**  
  **개인 Docker Hub는 쓰지 말 것.** 인수인계·감사가 불가하고 퇴사 시 접근이 끊깁니다.  
  **조직/팀 소유** 레지스트리를 사용하세요.
  - 사용 레지스트리: **팀·회사 계정**의 Docker Hub Organization, GitHub Container Registry(org), GitLab Registry, 또는 사내 레지스트리(Harbor, ECR, GCR 등)
  - 레포지토리 URL (예: `ghcr.io/회사명/newtechtest-app`, `docker.io/회사팀이름/newtechtest-app`)
  - 로그인 계정·토큰은 **조직 비밀 저장소**에 보관. 서버/CI에서 pull 시 이 계정 사용.
- [ ] **환경 변수·시크릿**
  - `JWT_SECRET` (운영용) — 실제 값은 비밀 저장소에, 여기서는 “어디에 있는지”만 기록
  - DB 사용 시: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 보관 위치
- [ ] **CI/CD**
  - 사용 툴 (GitHub Actions / GitLab CI / Jenkins 등)
  - 설정 파일 경로 (예: `.github/workflows/deploy.yml`)
  - 빌드·배포 절차 요약 (빌드 → push → 서버에서 pull & run)
- [ ] **모니터링·로그**
  - 로그 확인 방법 (아래 4절)
  - 알람/모니터링이 있다면 대시보드·연락처

---

## 3. 일상 운영 (다음 관리자가 할 일)

### 3.1 Docker로 실행 (로컬/테스트)

```bash
# 프로젝트 루트에서
docker compose up -d app
# 접속: http://localhost
```

전체 서비스(backend, frontend 포함):

```bash
docker compose up -d
```

### 3.2 중지·재시작

```bash
docker compose down
docker compose up -d app
```

특정 서비스만 재시작:

```bash
docker compose restart app
```

### 3.3 이미지 업데이트 (레지스트리 사용 시)

서버에서 새 이미지 받아서 재기동:

```bash
docker pull <레지스트리>/newtechtest-app:latest
docker compose -f docker-compose.prod.yml up -d app   # 실제 운영용 compose가 있다면
# 또는
docker stop shop-app && docker rm shop-app
docker run -d -p 80:80 --name shop-app <레지스트리>/newtechtest-app:latest
```

(실제 레지스트리 주소·compose 파일명은 위 2절 체크리스트에 적어 둡니다.)

### 3.4 환경 변수 (운영)

- **JWT_SECRET**: 반드시 32자 이상 랜덤 값으로 변경해 사용.
- **H2 → MariaDB** 전환 시: `docker-compose.yml`에 mariadb 서비스 추가 후,  
  `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 설정.

---

## 4. 로그·문제 해결

### 4.1 시연·라이브 서버 (demo / prod 프로필)

Spring Boot는 **호스트 파일**과 **Docker stdout** 둘 다에 기록합니다 (`logback-spring.xml`, `application-demo.yml` / `application-prod.yml`).

| 항목 | 경로 |
|------|------|
| 현재 로그 | `${DEPLOY_ROOT}/logs/application.log` (기본: `/var/www/html/tcgshop/logs/application.log`) |
| 롤링 아카이브 | `${DEPLOY_ROOT}/logs/application.yyyy-MM-dd.*.log.gz` |
| 컨테이너 내부 | `/app/logs/` (`docker-compose.demo.yml` 볼륨 마운트) |

배포 전 호스트에 로그 디렉터리 생성:

```bash
mkdir -p /var/www/html/tcgshop/logs
```

조회 예:

```bash
tail -f /var/www/html/tcgshop/logs/application.log
grep -i error /var/www/html/tcgshop/logs/application.log | tail -50
ls -la /var/www/html/tcgshop/logs/
```

라이브 서버(`SPRING_PROFILES_ACTIVE=prod`)도 동일하게 `${DEPLOY_ROOT}/logs:/app/logs` 볼륨만 추가하면 됩니다.

### 4.2 로컬 Docker / 개발 (dev 프로필)

파일 로그 없음. 필요 시:

```bash
# app 컨테이너 로그 (실시간)
docker compose logs -f app

# 최근 200줄
docker compose logs app | tail -200
```

자주 나오는 상황:

- **포트 사용 중**: `docker compose down` 후 다시 `up`.  
  로컬에서 같은 포트를 쓰는 프로세스(예: `bootRun`, `next dev`)가 있으면 중지하거나, compose의 호스트 포트를 변경.
- **이미지 빌드 실패**: `docker compose build --no-cache app` 후 재시도.
- **H2 사용**: 컨테이너 재시작 시 DB 초기화됨(테스트용).  
  데이터 유지가 필요하면 MariaDB 등 외부 DB로 전환.

---

## 5. 이관 시 새 관리자가 할 일

1. 이 문서(**OPERATIONS.md**)와 **README.md** 읽기.
2. 위 **2. 인수인계 체크리스트** 항목으로 접속 정보·레지스트리·시크릿 보관 위치 확인.
3. 로컬에서 `docker compose up -d app` 실행해 보며 접속( http://localhost ) 확인.
4. (사용 중이라면) CI/CD 파이프라인 한 번 실행해 보기.
5. 운영 서버가 있다면, 해당 서버에서 `docker pull` → 재기동 절차 한 번 수행해 보기.

---

## 6. 문서·코드 위치

- **프로젝트 루트**: `README.md` (개발·실행 요약), `docker-compose.yml`
- **운영 가이드**: 이 파일 `docs/OPERATIONS.md`
- **인수인계 시 내가 할 일**: **[docs/HANDOVER-ACTIONS.md](HANDOVER-ACTIONS.md)** — 소스·Docker·SSH 넘기는 구체적 행동 체크리스트
- **API·URL 규칙**: `.cursor/rules/` 아래 프로젝트 가이드 참고

관리 책임 이관 시 **HANDOVER-ACTIONS.md**를 따라 진행한 뒤, 이 문서(OPERATIONS.md)를 최신으로 갱신하고 새 관리자와 함께 위 체크리스트를 점검하면 됩니다.
