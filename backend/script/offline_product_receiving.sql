-- ============================================================
-- 입고 시스템 스키마 (PROD)
-- 대상 DB : MariaDB 10.x
-- 관련 엔티티:
--   OfflineProduct (receiving_quantity, shipping_quantity)
--   OfflineProductReceivingHistory
--   OfflineProductReceivingItem
-- ============================================================
--
-- 실행 방법
--   mysql -u <user> -p <database> < offline_product_receiving.sql
--
-- 주의
--   1. production 에서는 spring.jpa.hibernate.ddl-auto=validate(또는 none) 유지
--   2. 컬럼/테이블이 이미 있으면 해당 구문은 스킵하거나 에러 후 다음 구문 실행
--   3. stock_quantity 컬럼은 만들지 않음 (DTO에서 receiving - shipping 계산)
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1) offline_products: 입고/출고 누적 수량 컬럼
-- ------------------------------------------------------------
ALTER TABLE `offline_products`
    ADD COLUMN `receiving_quantity` INT NULL COMMENT '입고 누적 수량',
    ADD COLUMN `shipping_quantity`  INT NULL COMMENT '출고 누적 수량';

-- ------------------------------------------------------------
-- 2) 입고 이력 헤더
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `offline_product_receiving_histories` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`         DATETIME     NULL,
    `receiving_manager`  VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_offline_product_receiving_histories_created_at`
    ON `offline_product_receiving_histories` (`created_at`);

-- ------------------------------------------------------------
-- 3) 입고 이력 품목
--    product_id = offline_products.id (Toss productId 문자열 아님)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `offline_product_receiving_items` (
    `id`                                     BIGINT NOT NULL AUTO_INCREMENT,
    `offline_product_receiving_history_id`   BIGINT NOT NULL,
    `product_id`                             BIGINT NOT NULL,
    `receiving_quantity`                     INT    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_receiving_items_history_id` (`offline_product_receiving_history_id`),
    KEY `idx_receiving_items_product_id` (`product_id`),
    CONSTRAINT `fk_receiving_items_history`
        FOREIGN KEY (`offline_product_receiving_history_id`)
        REFERENCES `offline_product_receiving_histories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
