-- ============================================================
-- offline_products.shipping_quantity 전량 재집계 백필
-- 대상 DB : MariaDB 10.x
--
-- 용도
--   출고(shipping) 로직 도입(2026-07-29) 이전 COMPLETED 매출이
--   shipping_quantity에 반영되지 않은 cutover 공백 보정.
--   절대값 SET 이므로 필요 시 재실행해도 idempotent.
--
-- 규칙 (런타임 OfflineShippingQuantityAdjuster 와 동일)
--   1. order_state = 'COMPLETED' 인 주문 라인만 합산
--   2. OfflineSalesItem.title ↔ OfflineProduct.title 매칭
--   3. 동일 title 복수 상품이면 MIN(id) 첫 건만 출고 반영, 나머지는 0
--   4. 매출 매칭이 없는 상품은 shipping_quantity = 0
--
-- 실행 방법
--   mysql -u <user> -p <database> < offline_product_shipping_quantity_backfill.sql
--   (사전 SELECT 확인 후 UPDATE 구간만 실행해도 됨)
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1) 사전 검증: title별 COMPLETED 집계 vs 현재 shipping_quantity (diff)
--    first_product 기준. diff != 0 인 건만 조회.
-- ------------------------------------------------------------
SELECT
    op.id AS product_id,
    op.title,
    COALESCE(op.shipping_quantity, 0) AS current_shipping,
    COALESCE(agg.qty, 0) AS expected_shipping,
    COALESCE(agg.qty, 0) - COALESCE(op.shipping_quantity, 0) AS diff,
    COALESCE(op.receiving_quantity, 0) AS receiving,
    COALESCE(op.receiving_quantity, 0) - COALESCE(agg.qty, 0) AS stock_after
FROM offline_products op
INNER JOIN (
    SELECT title, MIN(id) AS first_id
    FROM offline_products
    WHERE title IS NOT NULL AND title <> ''
    GROUP BY title
) first_op ON first_op.first_id = op.id
LEFT JOIN (
    SELECT i.title AS title, COALESCE(SUM(i.quantity), 0) AS qty
    FROM offline_sales_items i
    INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
    WHERE o.order_state = 'COMPLETED'
      AND i.title IS NOT NULL
      AND i.title <> ''
    GROUP BY i.title
) agg ON agg.title = op.title
WHERE COALESCE(agg.qty, 0) <> COALESCE(op.shipping_quantity, 0)
ORDER BY ABS(COALESCE(agg.qty, 0) - COALESCE(op.shipping_quantity, 0)) DESC, op.title;

-- ------------------------------------------------------------
-- 2) 사전 검증: 매출 title 중 OfflineProduct 미매칭
-- ------------------------------------------------------------
SELECT
    agg.title,
    agg.qty AS completed_quantity
FROM (
    SELECT i.title AS title, COALESCE(SUM(i.quantity), 0) AS qty
    FROM offline_sales_items i
    INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
    WHERE o.order_state = 'COMPLETED'
      AND i.title IS NOT NULL
      AND i.title <> ''
    GROUP BY i.title
) agg
LEFT JOIN offline_products op ON op.title = agg.title
WHERE op.id IS NULL
ORDER BY agg.qty DESC, agg.title;

-- ------------------------------------------------------------
-- 3) 사전 검증: 동일 title 복수 OfflineProduct
-- ------------------------------------------------------------
SELECT
    title,
    COUNT(*) AS product_count,
    GROUP_CONCAT(id ORDER BY id) AS product_ids
FROM offline_products
WHERE title IS NOT NULL AND title <> ''
GROUP BY title
HAVING COUNT(*) > 1
ORDER BY product_count DESC, title;

-- ------------------------------------------------------------
-- 4) UPDATE: 전량 절대값 SET
--    - title 그룹의 MIN(id) 상품 → COMPLETED 합(없으면 0)
--    - 그 외(중복 title·빈 title 포함) → 0
-- ------------------------------------------------------------
START TRANSACTION;

UPDATE offline_products op
LEFT JOIN (
    SELECT title, MIN(id) AS first_id
    FROM offline_products
    WHERE title IS NOT NULL AND title <> ''
    GROUP BY title
) first_op ON first_op.first_id = op.id
LEFT JOIN (
    SELECT i.title AS title, COALESCE(SUM(i.quantity), 0) AS qty
    FROM offline_sales_items i
    INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
    WHERE o.order_state = 'COMPLETED'
      AND i.title IS NOT NULL
      AND i.title <> ''
    GROUP BY i.title
) agg ON agg.title = op.title AND first_op.first_id = op.id
SET op.shipping_quantity = CASE
    WHEN first_op.first_id IS NOT NULL AND first_op.first_id = op.id
        THEN COALESCE(agg.qty, 0)
    ELSE 0
END;

COMMIT;

-- ------------------------------------------------------------
-- 5) 사후 검증: first_product 기준 diff 가 0건이어야 함
-- ------------------------------------------------------------
SELECT COUNT(*) AS remaining_diff_count
FROM offline_products op
INNER JOIN (
    SELECT title, MIN(id) AS first_id
    FROM offline_products
    WHERE title IS NOT NULL AND title <> ''
    GROUP BY title
) first_op ON first_op.first_id = op.id
LEFT JOIN (
    SELECT i.title AS title, COALESCE(SUM(i.quantity), 0) AS qty
    FROM offline_sales_items i
    INNER JOIN offline_sales_infos o ON i.offline_sales_info_id = o.id
    WHERE o.order_state = 'COMPLETED'
      AND i.title IS NOT NULL
      AND i.title <> ''
    GROUP BY i.title
) agg ON agg.title = op.title
WHERE COALESCE(agg.qty, 0) <> COALESCE(op.shipping_quantity, 0);

-- ------------------------------------------------------------
-- 6) 사후 점검: 재고(receiving - shipping) 음수 건
-- ------------------------------------------------------------
SELECT
    id,
    title,
    COALESCE(receiving_quantity, 0) AS receiving,
    COALESCE(shipping_quantity, 0) AS shipping,
    COALESCE(receiving_quantity, 0) - COALESCE(shipping_quantity, 0) AS stock
FROM offline_products
WHERE COALESCE(receiving_quantity, 0) - COALESCE(shipping_quantity, 0) < 0
ORDER BY stock ASC, title;
