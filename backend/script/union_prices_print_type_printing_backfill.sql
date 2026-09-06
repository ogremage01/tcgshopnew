-- union_prices print_type / printing 백필 (1회)
-- 소스 테이블 JOIN으로 check_code_refined 기준 정정.
-- 권장 순서: union_prices → card_product → product_search_maps

-- ---------------------------------------------------------------------------
-- 0) 적용 전 미리보기 (선택)
-- ---------------------------------------------------------------------------
-- SELECT u.id, u.game, u.image_source, u.print_type, u.printing, f.foil
-- FROM union_prices u
-- JOIN fab_prices f ON f.check_code_refined = u.check_code_refined
-- WHERE u.game = 'Flesh & Blood TCG' AND u.image_source = 'OPB'
-- LIMIT 200;

-- ---------------------------------------------------------------------------
-- 1) union_prices
-- ---------------------------------------------------------------------------

-- MTG (Open Binder)
UPDATE union_prices u
JOIN mtg_prices m ON m.check_code_refined = u.check_code_refined
SET u.print_type = m.type,
    u.printing = m.type
WHERE u.game = 'Magic: The Gathering'
  AND u.image_source = 'OPB';

-- FAB (Open Binder)
-- fab_prices.foil: 'Normal' | 'Rainbow Foil' 등
UPDATE union_prices u
JOIN fab_prices f ON f.check_code_refined = u.check_code_refined
SET u.print_type = CASE
        WHEN LOWER(COALESCE(f.foil, '')) LIKE '%foil%' THEN 'Foil'
        ELSE 'Normal'
    END,
    u.printing = CASE
        WHEN LOWER(COALESCE(f.foil, '')) LIKE '%foil%' THEN f.foil
        ELSE 'Normal'
    END
WHERE u.game = 'Flesh & Blood TCG'
  AND u.image_source = 'OPB';

-- TCGPlayer (FAB 제외)
UPDATE union_prices u
JOIN tcg_p_prices t ON t.check_code_refined = u.check_code_refined
SET u.print_type = t.print_type,
    u.printing = t.printing
WHERE u.image_source = 'TCGP'
  AND (t.game IS NULL OR t.game <> 'Flesh & Blood TCG');

-- ---------------------------------------------------------------------------
-- 2) card_product
-- ---------------------------------------------------------------------------
UPDATE card_product cp
JOIN union_prices up ON cp.union_price_id = up.id
SET cp.print_type = up.print_type;

-- ---------------------------------------------------------------------------
-- 3) product_search_maps
-- ---------------------------------------------------------------------------
UPDATE product_search_maps psm
JOIN union_prices up ON psm.source_id = up.id
SET psm.print_type = up.print_type
WHERE psm.table_name = 'UNION_PRICE';

UPDATE product_search_maps psm
JOIN card_product cp ON psm.source_id = cp.id
SET psm.print_type = cp.print_type
WHERE psm.table_name = 'CARD_PRODUCT';

-- ---------------------------------------------------------------------------
-- 4) 적용 후 검증 (선택)
-- ---------------------------------------------------------------------------
-- WTR-024-Normal-Unlimited Edition → Normal / Normal
-- SELECT u.print_type, u.printing, f.foil
-- FROM union_prices u
-- JOIN fab_prices f ON f.check_code_refined = u.check_code_refined
-- WHERE f.check_code_refined = 'WTR-024-Normal-Unlimited Edition';

-- SELECT print_type, printing, COUNT(*) AS cnt
-- FROM union_prices
-- WHERE game = 'Flesh & Blood TCG'
-- GROUP BY print_type, printing
-- ORDER BY cnt DESC;
