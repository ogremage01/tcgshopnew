# ========== 백엔드 빌드 ==========
FROM gradle:8-jdk17-alpine AS backend-build
WORKDIR /app
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
COPY backend/src src
RUN gradle bootJar --no-daemon -x test

# ========== 프론트엔드 빌드 (동일 호스트 /api 사용 → baseURL 빈 값) ==========
FROM node:22-alpine AS frontend-build
WORKDIR /app
ARG NEXT_PUBLIC_SITE_URL=
ENV NEXT_PUBLIC_API_URL=
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}
RUN npm install -g npm@latest
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts
COPY frontend/ .
RUN npm run build

# ========== 통합 런타임: 한 컨테이너에서 백엔드 + 프론트 + Nginx ==========
# 최신 22.x-alpine 사용 시 Alpine 패키지(busybox, zlib) 수정 버전 반영 가능
FROM node:22-alpine
WORKDIR /app

# Alpine 보안 업데이트(busybox, zlib 등) + npm 최신화(minimatch CVE 대응)
RUN apk update && apk upgrade --no-cache \
    && apk add --no-cache openjdk17-jre-headless nginx \
    && npm install -g npm@latest

# 백엔드 JAR
COPY --from=backend-build /app/build/libs/*.jar /app/backend/app.jar

# 프론트엔드 standalone
COPY --from=frontend-build /app/public /app/frontend/public
COPY --from=frontend-build /app/.next/standalone /app/frontend/
COPY --from=frontend-build /app/.next/static /app/frontend/.next/static

# Nginx 설정 및 시작 스크립트
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/start.sh /app/start.sh
RUN chmod +x /app/start.sh \
    && mkdir -p /app/logs

EXPOSE 80

ENTRYPOINT ["/app/start.sh"]
