# Docker tar 배포 가이드

이 문서는 `tcgshop-app.tar` 파일을 서버에 업로드한 뒤 `docker load` 해서 실행하는 방식만 기준으로 정리한 가이드입니다.

운영 서버는 `docker-compose.prod.yml` 기준으로 설명합니다.  
`docker-compose.demo.yml`은 데모/스테이징용으로 따로 둡니다.

## 한 줄 결론

이 프로젝트는 서버에 `Java`, `Node`, `npm`, `Gradle` 을 따로 설치할 필요가 없습니다.

서버에는 아래만 있으면 됩니다.

- `Docker Engine`
- `Docker Compose Plugin`
- 별도 `MariaDB` 서버 또는 같은 서버의 MariaDB
- `.env` 파일
- 업로드/이미지/로그 저장 디렉터리

## 1. 서버에 필요한 것

필수:

- Docker
- Docker Compose
- MariaDB 연결 정보

선택:

- 서버 앞단 Nginx
- 도메인 / HTTPS

권장 서버:

- Ubuntu 24.04 LTS
- 2 vCPU
- 4GB RAM

## 2. Docker 설치

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

확인:

```bash
docker version
docker compose version
```

## 3. 서버 폴더 만들기

```bash
sudo mkdir -p /var/www/html/tcgshop
sudo chown -R $USER:$USER /var/www/html/tcgshop
cd /var/www/html/tcgshop
mkdir -p data/uploads data/card-images logs
```

## 4. 서버에 올릴 파일

서버에는 최소 아래 파일들이 있어야 합니다.

- `tcgshop-app.tar`
- `docker-compose.prod.yml`
- `.env`
- `nginx.conf`

보통은 로컬 저장소에서 아래 파일들을 같이 업로드하면 됩니다.

- `tcgshop-app.tar`
- `docker-compose.prod.yml`
- `deploy/example/.env.server.prod.example`
- `docker/nginx.conf`

## 5. .env 만들기

서버에서:

```bash
cd /var/www/html/tcgshop
cp .env.server.prod.example .env
```

예시 파일:

- `deploy/example/.env.server.prod.example`

최소 수정할 값:

- `DB_PASSWORD`
- `JWT_SECRET`
- `PERSONAL_DATA_ENCRYPTION_KEY`
- `CORS_ORIGINS`
- `NEXT_PUBLIC_SITE_URL`
- `GOOGLE_MAIL_ADDRESS`
- `GOOGLE_MAIL_PASSWORD`

현재 서버처럼 MariaDB가 같은 서버에 있고 실제 사용 DB가 `shop` 라면 아래 예시를 그대로 시작점으로 쓰면 됩니다.

```dotenv
DEPLOY_ROOT=/var/www/html/tcgshop
HTTP_PORT=2602

SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:mariadb://127.0.0.1:3306/shop?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul
DB_USERNAME=shopadmin
DB_PASSWORD=change-me

JWT_SECRET=change-me-to-at-least-32-characters-long-secret
PERSONAL_DATA_ENCRYPTION_KEY=change-me-base64-32-bytes

CORS_ORIGINS=https://your-domain.com
NEXT_PUBLIC_SITE_URL=https://your-domain.com
STRICT_ORIGIN_CHECK=true

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
GOOGLE_MAIL_ADDRESS=your@gmail.com
GOOGLE_MAIL_PASSWORD=your-app-password
```

## 6. nginx.conf 준비

현재 Compose는 서버 경로의 `nginx.conf` 를 컨테이너 안으로 마운트합니다.

서버에서:

```bash
cp docker/nginx.conf /var/www/html/tcgshop/nginx.conf
```

현재 기본 포트:

```nginx
listen 2602;
```

즉 기본 접속 예시는:

```text
http://your-domain.com:2602
```

## 7. DB 준비

이 구성은 DB를 Docker로 같이 띄우지 않습니다.

현재 서버처럼 MariaDB가 같은 서버에 있으면:

```dotenv
DB_URL=jdbc:mariadb://127.0.0.1:3306/shop?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul
```

별도 DB 서버라면:

```dotenv
DB_URL=jdbc:mariadb://db-host.example.com:3306/shop?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul
```

스키마 파일:

- `docs/table/create_tables.sql`

현재 서버 기준 스키마 적용:

```bash
mysql -u shopadmin -p -h 127.0.0.1 -D shop < docs/table/create_tables.sql
```

## 8. tar 로드 후 실행

서버에서:

```bash
cd /var/www/html/tcgshop
docker load -i tcgshop-app.tar
docker compose -f docker-compose.prod.yml up -d
```

확인:

```bash
docker ps
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f
```

앱 로그:

```text
/var/www/html/tcgshop/logs/application.log
```

## 9. 실제 실행 구조

컨테이너 안에서 같이 실행됩니다.

- Spring Boot 백엔드
- Next.js 프론트엔드
- Nginx

포트 구조:

- backend: `18567`
- frontend: `3000`
- nginx: `2602`

외부에서는 `2602` 로 붙고, 컨테이너 내부 Nginx가 `/api` 는 백엔드로, 나머지는 프론트로 넘깁니다.

## 10. 80/443으로 받고 싶을 때

실서비스면 보통 서버 앞단 Nginx를 하나 더 둡니다.

흐름:

- 사용자 접속: `80/443`
- 서버 Nginx
- Docker 앱: `127.0.0.1:2602`

예시 파일:

- `deploy/example/nginx-proxy.conf.example`

## 11. 업데이트 방법

새 tar 파일로 교체할 때:

```bash
cd /var/www/html/tcgshop
docker load -i tcgshop-app.tar
docker compose -f docker-compose.prod.yml up -d
```

정리:

```bash
docker image prune -f
```

## 12. 서버에서 실제로 치는 최소 명령

처음 1회:

```bash
mkdir -p /var/www/html/tcgshop/data/uploads /var/www/html/tcgshop/data/card-images /var/www/html/tcgshop/logs
cd /var/www/html/tcgshop
cp .env.server.prod.example .env
cp docker/nginx.conf /var/www/html/tcgshop/nginx.conf
docker load -i tcgshop-app.tar
docker compose -f docker-compose.prod.yml up -d
```

업데이트:

```bash
cd /var/www/html/tcgshop
docker load -i tcgshop-app.tar
docker compose -f docker-compose.prod.yml up -d
```

## 13. 핵심 요약

- 운영은 `docker-compose.prod.yml`
- 데모/스테이징은 `docker-compose.demo.yml`
- 서버에 `Java`, `Node` 설치 안 해도 됩니다.
- 서버에는 `Docker`만 있으면 앱은 뜹니다.
- 대신 `MariaDB`, `.env`, 저장 디렉터리, 필요하면 앞단 Nginx는 따로 준비해야 합니다.
