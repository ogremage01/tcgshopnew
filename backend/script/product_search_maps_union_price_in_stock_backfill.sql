-- UNION_PRICE 대표 검색맵 in_stock 집계 백필 (1회)
-- 조건: visible·미삭제 CardProduct 중 current_visible_stock > 0 이 하나라도 있으면 true
--
-- 실행 전 불일치 건수 확인 (4.2 검증 쿼리와 동일)
-- SELECT COUNT(*) FROM union_prices up
-- JOIN product_search_maps psm ON psm.table_name = 'UNION_PRICE' AND psm.source_id = up.id
-- WHERE EXISTS (
--     SELECT 1 FROM card_product cp
--     WHERE cp.union_price_id = up.id
--       AND cp.is_visible = TRUE
--       AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
--       AND COALESCE(cp.current_visible_stock, 0) > 0
-- ) AND COALESCE(psm.in_stock, FALSE) = FALSE;

UPDATE product_search_maps psm
LEFT JOIN (
    SELECT cp.union_price_id
    FROM card_product cp
    WHERE cp.is_visible = TRUE
      AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
      AND COALESCE(cp.current_visible_stock, 0) > 0
    GROUP BY cp.union_price_id
) stocked ON stocked.union_price_id = psm.source_id
SET
    psm.in_stock = (stocked.union_price_id IS NOT NULL),
    psm.updated_at = NOW()
WHERE psm.table_name = 'UNION_PRICE';

-- 실행 후 불일치 0건 확인
-- SELECT COUNT(*) FROM union_prices up
-- JOIN product_search_maps psm ON psm.table_name = 'UNION_PRICE' AND psm.source_id = up.id
-- WHERE EXISTS (
--     SELECT 1 FROM card_product cp
--     WHERE cp.union_price_id = up.id
--       AND cp.is_visible = TRUE
--       AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
--       AND COALESCE(cp.current_visible_stock, 0) > 0
-- ) AND COALESCE(psm.in_stock, FALSE) = FALSE;
