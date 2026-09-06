-- Development-only reset for UnionPrice mtg-kr V2 migration.
-- This removes FAB from TCGPlayer sync targets and clears card catalog/product rows
-- so UnionPrice can be rebuilt from mtg-kr/OpenBinder sources.

DELETE FROM tcg_p_sync_games
WHERE product_line_name = 'Flesh & Blood TCG'
   OR product_line_id = 62;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM product_search_maps
WHERE table_name IN ('UNION_PRICE', 'CARD_PRODUCT');

TRUNCATE TABLE card_product;
TRUNCATE TABLE union_prices;

SET FOREIGN_KEY_CHECKS = 1;
