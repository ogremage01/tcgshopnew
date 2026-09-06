-- 수동 상품 검색 맵: manual_category / game 비정규화 백필 (1회)

UPDATE product_search_maps psm
INNER JOIN manual_products mp
    ON psm.source_id = mp.id AND psm.table_name = 'MANUAL_PRODUCT'
SET
    psm.manual_category = mp.product_type,
    psm.game = mp.product_ip,
    psm.product_type = 'ManualProducts'
WHERE psm.table_name = 'MANUAL_PRODUCT';
