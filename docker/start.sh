#!/bin/sh
set -e
mkdir -p /app/logs
# 백엔드·프론트엔드 기동 (백그라운드)
java -jar /app/backend/app.jar &
cd /app/frontend && node server.js &
# Spring Boot 기동 대기 후 Nginx 포그라운드 (즉시 요청 시 502 방지)
sleep 15
exec nginx -g "daemon off;"
