-- sealed_product.current_visble_stock → current_visible_stock (필드명 오타 수정)
ALTER TABLE sealed_product
    CHANGE COLUMN current_visble_stock current_visible_stock INT NULL;

-- 기존 행: 표시 재고가 비어 있으면 total_stock 으로 초기화
UPDATE sealed_product
SET current_visible_stock = total_stock
WHERE current_visible_stock IS NULL
  AND total_stock IS NOT NULL;
