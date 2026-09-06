-- UNION_PRICE 대표 검색맵 중 cardName에 (Art) 포함 항목 숨김 (1회)
--
-- 실행 전 노출 중인 (Art) 대표 행 건수 확인
-- SELECT COUNT(*)
-- FROM product_search_maps psm
-- JOIN union_prices up ON up.id = psm.source_id
-- WHERE psm.table_name = 'UNION_PRICE'
--   AND COALESCE(psm.is_visible, TRUE) = TRUE
--   AND up.card_name LIKE '%(Art)%';

UPDATE product_search_maps psm
JOIN union_prices up ON up.id = psm.source_id
SET
    psm.is_visible = FALSE,
    psm.updated_at = NOW()
WHERE psm.table_name = 'UNION_PRICE'
  AND up.card_name LIKE '%(Art)%';

-- 실행 후 노출 중인 (Art) 대표 행 0건 확인
-- SELECT COUNT(*)
-- FROM product_search_maps psm
-- JOIN union_prices up ON up.id = psm.source_id
-- WHERE psm.table_name = 'UNION_PRICE'
--   AND COALESCE(psm.is_visible, TRUE) = TRUE
--   AND up.card_name LIKE '%(Art)%';
