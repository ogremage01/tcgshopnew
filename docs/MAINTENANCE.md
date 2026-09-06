# 유지보수·추후 개선 (Maintenance & Future Improvements)

의존성 업데이트, 취약점 대응, 주기 점검 방법을 정리한 문서입니다.

---

## 1. 추후 개선 요망점

### 의존성·취약점 관리

- **취약점은 지속적으로 발표됨** — 한 번 수정해도 새 CVE가 나오면 다시 업데이트가 필요함.
- 아래 **주기 점검**과 **자동화**를 함께 쓰면 부담을 줄일 수 있음.

### 권장 개선

| 항목 | 내용 |
|------|------|
| **자동 알림** | GitHub **Dependabot** 또는 **Renovate** 활성화 → 새 CVE/업데이트 시 PR로 제안받기. |
| **주기 점검** | 분기 1회 또는 배포 전에 의존성 점검·이미지 재빌드 (아래 2절 참고). |
| **메이저/패치 올리기** | Spring Boot 패치(4.0.x), Node 베이스 이미지 태그 업데이트 시 하위 라이브러리(Jackson, Tomcat, npm, Alpine 패키지)가 함께 갱신되어 CVE가 한꺼번에 해소되는 경우가 많음. |

---

## 2. 주기 점검 방법

### Backend (Gradle)

```bash
cd backend
./gradlew dependencyUpdates
```

- 결과: `build/dependencyUpdates/report.txt` 및 콘솔 출력.
- 업데이트할 항목을 골라 `build.gradle.kts` 버전 수정 후 테스트·배포.

### Frontend (npm)

```bash
cd frontend
npm audit
# 필요 시: npm audit fix (호환 범위 내 자동 수정)
```

### Docker 이미지

- `docker compose build app` 으로 앱 이미지 재빌드.
- Docker Scout(또는 사용 중인 스캔 도구)로 이미지 스캔 후 취약점 확인.
- **베이스 이미지 태그** 주기적 상향: 예) `node:22-alpine` → `node:22.13-alpine` 등.

---

## 3. 현재 CVE 대응 방식 (참고)

- **Backend (Jackson 등)**: `backend/build.gradle.kts` 의 `configurations.all { resolutionStrategy.eachDependency { ... } }` 에서 수정 버전 강제.
- **Alpine 패키지(busybox, zlib 등)**: `docker/Dockerfile` 런타임 스테이지에서 `apk update && apk upgrade` 로 최신 보안 패키지 반영.
- **npm 번들(tar, minimatch 등)**: 동일 Dockerfile에서 `npm install -g npm@latest` 로 npm 갱신.

새 CVE가 나오면 위와 동일한 방식으로 해당 의존성만 버전 고정 또는 베이스/이미지 업데이트를 추가하면 됨.

---

## 4. 남은 취약점·수용 가능 여부

스캔 후에도 아래와 같이 남는 경우가 있을 수 있음.

| 패키지 | 원인 | 놔둬도 되는가 | 비고 |
|--------|------|----------------|------|
| **npm / minimatch** | npm 번들 내부 의존성. npm이 10.2.3 이상으로 올리기 전까지 우리가 강제 불가. | **단기 수용 가능** | 런타임에 `npm install`/minimatch를 호출하지 않으므로 실제 공격 표면은 제한적. npm 새 버전 나오면 이미지 재빌드로 제거. |
| **apk / busybox** | Alpine 베이스 패키지. Alpine 레포에 수정 버전이 아직 없을 수 있음. | **단기 수용 가능** | 앱이 busybox(wget 등)를 직접 사용하지 않음. 베이스 이미지 태그를 최신으로 두고 주기 재빌드. |
| **apk / zlib** | 동일. `apk upgrade` 후에도 수정이 레포에 없으면 남음. | **수용 가능** | 2.9 Low. Alpine/node 이미지 업데이트 시 함께 해소. |

- **정책**상 High/Medium을 0으로 둬야 하면 “놔두기”가 아니라, 베이스 이미지·업스트림(node, Alpine) 업데이트를 계속 따라가며 재빌드해야 함.
- **실제 위험도**는 이 이미지가 **빌드가 아닌 런타임 전용**이라, npm·busybox 코드가 실행될 일이 거의 없어 영향이 제한적임.

---

## 5. 관련 문서

- **[TECH-STACK.md](../TECH-STACK.md)** — 현재 기술 스택·추천 라이브러리.
- **[OPERATIONS.md](OPERATIONS.md)** — 운영·배포 가이드.
- **[HANDOVER-ACTIONS.md](HANDOVER-ACTIONS.md)** — 인수인계 시 체크리스트.

---

*Dockerfile에서는 `node:22.13-alpine` 등 **구체적인 최신 태그**를 쓰면 Alpine 패키지(busybox, zlib) 수정 버전이 더 빨리 반영될 수 있음. 주기적으로 태그를 올려서 재빌드하면 남은 CVE가 줄어든다.*
