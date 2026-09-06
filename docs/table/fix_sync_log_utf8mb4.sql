-- sync_log 한글 메시지 저장 오류(MariaDB 1366 Incorrect string value) 수정용
-- 기존 DB에서 테이블/컬럼이 latin1 등으로 생성된 경우 실행한다.
-- 사용 전 대상 DB를 선택한다. 예: mysql -u root -p shop < fix_sync_log_utf8mb4.sql

ALTER TABLE `sync_log` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
