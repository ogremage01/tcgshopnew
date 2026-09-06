-- ============================================================
-- offline_products 연결 끊기 백필
-- 대상 DB : MariaDB 10.x
--
-- 용도
--   offline_products.link_table_name / link_id 가 가리키는
--   온라인 상품(Sealed / Manual / Supply)이 삭제(soft)되었거나
--   행 자체가 없으면 연결을 해제한다.
--   런타임 unlinkOfflineProduct 와 동일: link_table_name, link_id → NULL
--
-- 규칙
--   1. link_table_name IN ('Sealed','Manual','Supply') 이고 link_id 가 있는 행만 대상
--   2. 대상 행 없음(하드 삭제/유실) 또는 is_deleted = 1 → 연결 해제
--   3. 재실행해도 idempotent
--
-- 실행 방법
--   mysql -u <user> -p <database> < offline_product_unlink_deleted_backfill.sql
--   (사전 SELECT 확인 후 UPDATE 구간만 실행해도 됨)
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1) 사전 검증: 삭제/미존재 연결 목록
-- ------------------------------------------------------------
SELECT
    op.id,
    op.product_id,
    op.title,
    op.link_table_name,
    op.link_id,
    CASE
        WHEN op.link_table_name = 'Sealed' AND sp.id IS NULL THEN 'MISSING'
        WHEN op.link_table_name = 'Sealed' AND sp.is_deleted = 1 THEN 'SOFT_DELETED'
        WHEN op.link_table_name = 'Manual' AND mp.id IS NULL THEN 'MISSING'
        WHEN op.link_table_name = 'Manual' AND mp.is_deleted = 1 THEN 'SOFT_DELETED'
        WHEN op.link_table_name = 'Supply' AND su.id IS NULL THEN 'MISSING'
        WHEN op.link_table_name = 'Supply' AND su.is_deleted = 1 THEN 'SOFT_DELETED'
        ELSE 'OK'
    END AS unlink_reason
FROM offline_products op
LEFT JOIN sealed_product sp
    ON op.link_table_name = 'Sealed' AND sp.id = op.link_id
LEFT JOIN manual_products mp
    ON op.link_table_name = 'Manual' AND mp.id = op.link_id
LEFT JOIN supplies su
    ON op.link_table_name = 'Supply' AND su.id = op.link_id
WHERE op.link_table_name IN ('Sealed', 'Manual', 'Supply')
  AND op.link_id IS NOT NULL
  AND (
        (op.link_table_name = 'Sealed' AND (sp.id IS NULL OR sp.is_deleted = 1))
     OR (op.link_table_name = 'Manual' AND (mp.id IS NULL OR mp.is_deleted = 1))
     OR (op.link_table_name = 'Supply' AND (su.id IS NULL OR su.is_deleted = 1))
  )
ORDER BY op.link_table_name, op.id;

-- ------------------------------------------------------------
-- 2) 사전 집계: 테이블별 해제 예정 건수
-- ------------------------------------------------------------
SELECT
    op.link_table_name,
    COUNT(*) AS unlink_count
FROM offline_products op
LEFT JOIN sealed_product sp
    ON op.link_table_name = 'Sealed' AND sp.id = op.link_id
LEFT JOIN manual_products mp
    ON op.link_table_name = 'Manual' AND mp.id = op.link_id
LEFT JOIN supplies su
    ON op.link_table_name = 'Supply' AND su.id = op.link_id
WHERE op.link_table_name IN ('Sealed', 'Manual', 'Supply')
  AND op.link_id IS NOT NULL
  AND (
        (op.link_table_name = 'Sealed' AND (sp.id IS NULL OR sp.is_deleted = 1))
     OR (op.link_table_name = 'Manual' AND (mp.id IS NULL OR mp.is_deleted = 1))
     OR (op.link_table_name = 'Supply' AND (su.id IS NULL OR su.is_deleted = 1))
  )
GROUP BY op.link_table_name
ORDER BY op.link_table_name;

-- ------------------------------------------------------------
-- 3) UPDATE: 연결 해제
-- ------------------------------------------------------------
START TRANSACTION;

UPDATE offline_products op
LEFT JOIN sealed_product sp
    ON op.link_table_name = 'Sealed' AND sp.id = op.link_id
LEFT JOIN manual_products mp
    ON op.link_table_name = 'Manual' AND mp.id = op.link_id
LEFT JOIN supplies su
    ON op.link_table_name = 'Supply' AND su.id = op.link_id
SET
    op.link_table_name = NULL,
    op.link_id = NULL
WHERE op.link_table_name IN ('Sealed', 'Manual', 'Supply')
  AND op.link_id IS NOT NULL
  AND (
        (op.link_table_name = 'Sealed' AND (sp.id IS NULL OR sp.is_deleted = 1))
     OR (op.link_table_name = 'Manual' AND (mp.id IS NULL OR mp.is_deleted = 1))
     OR (op.link_table_name = 'Supply' AND (su.id IS NULL OR su.is_deleted = 1))
  );

COMMIT;

-- ------------------------------------------------------------
-- 4) 사후 검증: 남아 있으면 안 됨 (0건)
-- ------------------------------------------------------------
SELECT COUNT(*) AS remaining_orphan_link_count
FROM offline_products op
LEFT JOIN sealed_product sp
    ON op.link_table_name = 'Sealed' AND sp.id = op.link_id
LEFT JOIN manual_products mp
    ON op.link_table_name = 'Manual' AND mp.id = op.link_id
LEFT JOIN supplies su
    ON op.link_table_name = 'Supply' AND su.id = op.link_id
WHERE op.link_table_name IN ('Sealed', 'Manual', 'Supply')
  AND op.link_id IS NOT NULL
  AND (
        (op.link_table_name = 'Sealed' AND (sp.id IS NULL OR sp.is_deleted = 1))
     OR (op.link_table_name = 'Manual' AND (mp.id IS NULL OR mp.is_deleted = 1))
     OR (op.link_table_name = 'Supply' AND (su.id IS NULL OR su.is_deleted = 1))
  );
