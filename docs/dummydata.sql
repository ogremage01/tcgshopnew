-- dummydata

-- DB 초기화 명령어: 
DROP DATABASE shop;

CREATE DATABASE shop
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;


-- GradePricingPolicy 테이블 테스트용 initial data
INSERT INTO grade_pricing_policies (grade, percentage) VALUES ('NM', 1.0);
INSERT INTO grade_pricing_policies (grade, percentage) VALUES ('EX', 0.9);
INSERT INTO grade_pricing_policies (grade, percentage) VALUES ('VG', 0.8);
INSERT INTO grade_pricing_policies (grade, percentage) VALUES ('G', 0.7);

-- tcg_p_sync_games 테이블 initial data
INSERT INTO tcg_p_sync_games (product_line_id, product_line_name) VALUES (62, 'Flesh and Blood TCG');
INSERT INTO tcg_p_sync_games (product_line_id, product_line_name) VALUES (71, 'Disney Lorcana');
INSERT INTO tcg_p_sync_games (product_line_id, product_line_name) VALUES (79, 'Star Wars: Unlimited');
INSERT INTO tcg_p_sync_games (product_line_id, product_line_name) VALUES (89, 'Riftbound: League of Legends Trading Card Game');

-- users 테이블 테스트용 더미 데이터 200건
-- MariaDB / MySQL 8+ (WITH RECURSIVE 지원)
-- 실행 전: 기존 테스트 데이터와 email 충돌이 없는지 확인

INSERT INTO users (
    name,
    email,
    password,
    role,
    created_at,
    updated_at,
    point,
    user_memo,
    user_status,
    auth_provider,
    terms_agreed,
    terms_agreed_at,
    terms_version,
    privacy_agreed,
    privacy_agreed_at,
    privacy_version
)
WITH RECURSIVE seq (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 200
)
SELECT
    CONCAT('User', LPAD(n, 3, '0')) AS name,
    CONCAT('user', LPAD(n, 3, '0'), '@example.com') AS email,
    CONCAT('password', LPAD(n, 3, '0')) AS password,
    'USER' AS role,
    DATE_ADD('2025-01-01 10:00:00', INTERVAL (n - 1) MINUTE) AS created_at,
    DATE_ADD('2025-01-01 10:00:00', INTERVAL (n - 1) MINUTE) AS updated_at,
    0 AS point,
    CONCAT('테스트 유저 ', n) AS user_memo,
    'ACTIVE' AS user_status,
    'LOCAL' AS auth_provider,
    1 AS terms_agreed,
    DATE_ADD('2025-01-01 10:00:00', INTERVAL (n - 1) MINUTE) AS terms_agreed_at,
    'v1.0' AS terms_version,
    1 AS privacy_agreed,
    DATE_ADD('2025-01-01 10:00:00', INTERVAL (n - 1) MINUTE) AS privacy_agreed_at,
    'v1.0' AS privacy_version
FROM seq;
