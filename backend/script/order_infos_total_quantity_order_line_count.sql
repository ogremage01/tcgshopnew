-- 주문 요약 집계 컬럼 (관리자 목록 등)
ALTER TABLE order_infos
    ADD COLUMN total_quantity BIGINT NULL COMMENT 'order_products.quantity 합계',
    ADD COLUMN order_line_count BIGINT NULL COMMENT 'order_products 행 개수(몇 종)';

-- 기존 행 백필(선택)
UPDATE order_infos o
SET total_quantity = (
        SELECT COALESCE(SUM(op.quantity), 0)
        FROM order_products op
        WHERE op.order_info_id = o.id
    ),
    order_line_count = (
        SELECT COUNT(*)
        FROM order_products op2
        WHERE op2.order_info_id = o.id
    );
