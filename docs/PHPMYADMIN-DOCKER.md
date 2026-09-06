# phpMyAdmin Docker 실행 가이드

이 문서는 `newtechtest` 앱과 별도로 `phpMyAdmin`을 Docker로 띄우는 방법입니다.

## 1. 준비 파일

추가한 파일:

- `deploy/example/phpmyadmin-compose.yml`
- `deploy/example/.env.phpmyadmin.example`

## 2. 서버에 파일 업로드

서버의 예시 경로:

```bash
mkdir -p /var/www/html/tcgshop/phpmyadmin
cd /var/www/html/tcgshop/phpmyadmin
```

아래 파일 2개를 서버에 올립니다.

- `deploy/example/phpmyadmin-compose.yml`
- `deploy/example/.env.phpmyadmin.example`

## 3. 환경변수 파일 만들기

```bash
cd /var/www/html/tcgshop/phpmyadmin
cp .env.phpmyadmin.example .env
```

예시:

```dotenv
PMA_HOST=db-host.example.com
PMA_PORT=3306
PHPMYADMIN_PORT=8080
PMA_ARBITRARY=0
PMA_UPLOAD_LIMIT=64M
```

설명:

- `PMA_HOST`: MariaDB 서버 주소
- `PMA_PORT`: MariaDB 포트
- `PHPMYADMIN_PORT`: phpMyAdmin을 열 포트
- `PMA_ARBITRARY=0`: 지정한 DB 서버만 접속 허용

## 4. 실행

```bash
cd /var/www/html/tcgshop/phpmyadmin
docker compose -f phpmyadmin-compose.yml --env-file .env up -d
```

확인:

```bash
docker ps
docker compose -f phpmyadmin-compose.yml --env-file .env ps
docker compose -f phpmyadmin-compose.yml --env-file .env logs -f
```

## 5. 접속

브라우저:

```text
http://서버IP:8080
```

또는:

```text
http://도메인:8080
```

로그인 값:

- 서버: 자동 입력됨
- 사용자명: DB 계정 예: `shopadmin`
- 비밀번호: DB 비밀번호

## 6. 중지 / 재시작

중지:

```bash
docker compose -f phpmyadmin-compose.yml --env-file .env down
```

재시작:

```bash
docker compose -f phpmyadmin-compose.yml --env-file .env up -d
```

## 7. 보안 권장

- 가능하면 `8080` 포트를 아무나 못 보게 방화벽 제한
- 가능하면 앞단 Nginx로 IP 제한
- `root` 대신 별도 DB 계정 사용
- 필요 없을 때는 컨테이너 중지
