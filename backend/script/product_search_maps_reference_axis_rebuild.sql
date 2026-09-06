-- ProductSearchMap reference-axis rebuild script.
--
-- Purpose:
--   Rebuild product_search_maps from the current source tables after table_name
--   was redefined as the actual referenced table.
--
-- Target DB:
--   MySQL / MariaDB
--
-- Important:
--   This script deletes every row in product_search_maps and recreates rows from
--   union_prices, card_product, sealed_product, and manual_products.
--   Run this in the development environment after clearing old cart, checkout,
--   and order data that may still point to legacy search_map_id values.

START TRANSACTION;

DELETE FROM product_search_maps;

-- Card catalog representative rows.
-- UNION_PRICE is display/catalog only. It is not a sale target.
INSERT INTO product_search_maps (
    product_name,
    product_name_ko,
    game,
    set_code,
    product_type,
    supplies_type,
    manual_category,
    print_type,
    in_stock,
    product_id,
    table_name,
    source_id,
    catalog_source_id,
    source_public_id,
    sort_price,
    is_visible,
    updated_at
)
SELECT
    up.card_name,
    up.card_namek,
    up.game,
    up.set_code,
    up.product_type,
    NULL,
    NULL,
    up.print_type,
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM card_product cp
            WHERE cp.union_price_id = up.id
              AND cp.is_visible = TRUE
              AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
              AND COALESCE(cp.current_visible_stock, 0) > 0
        ) THEN TRUE
        ELSE FALSE
    END,
    up.public_id,
    'UNION_PRICE',
    up.id,
    up.id,
    up.public_id,
    up.price,
    TRUE,
    NOW()
FROM union_prices up
WHERE up.public_id IS NOT NULL
  AND TRIM(up.public_id) <> ''
  AND (
      up.product_type IS NULL
      OR TRIM(up.product_type) = ''
      OR LOWER(TRIM(up.product_type)) LIKE 'cards%'
  )
  AND (up.card_name IS NULL OR up.card_name NOT LIKE '%(Art)%');

-- Actual card sale rows.
-- CARD_PRODUCT rows are sale options and inventory deduction targets.
INSERT INTO product_search_maps (
    product_name,
    product_name_ko,
    game,
    set_code,
    product_type,
    supplies_type,
    manual_category,
    print_type,
    in_stock,
    product_id,
    table_name,
    source_id,
    catalog_source_id,
    source_public_id,
    sort_price,
    is_visible,
    updated_at
)
SELECT
    up.card_name,
    up.card_namek,
    up.game,
    up.set_code,
    cp.product_type,
    NULL,
    NULL,
    cp.print_type,
    COALESCE(cp.current_visible_stock, 0) > 0,
    cp.public_id,
    'CARD_PRODUCT',
    cp.id,
    up.id,
    cp.public_id,
    up.price,
    cp.is_visible,
    NOW()
FROM card_product cp
JOIN union_prices up ON up.id = cp.union_price_id
WHERE cp.public_id IS NOT NULL
  AND TRIM(cp.public_id) <> '';

-- Sealed products are direct sale/display rows.
-- They do not connect to union_prices.
INSERT INTO product_search_maps (
    product_name,
    product_name_ko,
    game,
    set_code,
    product_type,
    supplies_type,
    manual_category,
    print_type,
    in_stock,
    product_id,
    table_name,
    source_id,
    catalog_source_id,
    source_public_id,
    sort_price,
    is_visible,
    updated_at
)
SELECT
    sp.product_name_en,
    sp.product_name_ko,
    sp.game,
    sp.set_code,
    'SealedProducts',
    NULL,
    NULL,
    NULL,
    COALESCE(sp.total_stock, 0) > 0,
    sp.public_id,
    'SEALED_PRODUCT',
    sp.id,
    NULL,
    sp.public_id,
    COALESCE(sp.price, 0),
    sp.is_active,
    NOW()
FROM sealed_product sp
WHERE sp.public_id IS NOT NULL
  AND TRIM(sp.public_id) <> '';

-- Manual products are direct sale/display rows.
INSERT INTO product_search_maps (
    product_name,
    product_name_ko,
    game,
    set_code,
    product_type,
    supplies_type,
    manual_category,
    print_type,
    in_stock,
    product_id,
    table_name,
    source_id,
    catalog_source_id,
    source_public_id,
    sort_price,
    is_visible,
    updated_at
)
SELECT
    mp.name_en,
    mp.name_ko,
    mp.product_ip,
    NULL,
    'ManualProducts',
    NULL,
    mp.product_type,
    NULL,
    COALESCE(mp.stock, 0) > 0,
    mp.public_id,
    'MANUAL_PRODUCT',
    mp.id,
    NULL,
    mp.public_id,
    COALESCE(mp.price, 0),
    mp.is_visible,
    NOW()
FROM manual_products mp
WHERE mp.public_id IS NOT NULL
  AND TRIM(mp.public_id) <> '';

COMMIT;

-- Verification queries.
--
-- 1. Card sale rows with stock should be in_stock = true.
-- SELECT cp.id, cp.public_id, cp.current_visible_stock, psm.id AS search_map_id, psm.in_stock
-- FROM card_product cp
-- JOIN product_search_maps psm
--   ON psm.table_name = 'CARD_PRODUCT'
--  AND psm.source_id = cp.id
-- WHERE COALESCE(cp.current_visible_stock, 0) > 0
--   AND psm.in_stock = FALSE;
--
-- 2. Catalog rows should reflect linked visible card sale stock.
-- SELECT up.id, up.public_id, psm.id AS search_map_id, psm.in_stock
-- FROM union_prices up
-- JOIN product_search_maps psm
--   ON psm.table_name = 'UNION_PRICE'
--  AND psm.source_id = up.id
-- WHERE EXISTS (
--     SELECT 1
--     FROM card_product cp
--     WHERE cp.union_price_id = up.id
--       AND cp.is_visible = TRUE
--       AND (cp.is_deleted = FALSE OR cp.is_deleted IS NULL)
--       AND COALESCE(cp.current_visible_stock, 0) > 0
-- )
--   AND psm.in_stock = FALSE;
