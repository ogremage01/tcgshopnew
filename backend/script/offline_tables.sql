-- ============================================================
-- 오프라인(상품·판매) 테이블 CREATE 스크립트
-- 대상 DB : MariaDB 10.x
-- 엔티티  : com.shop.offline.product.entity.OfflineProduct
--           com.shop.offline.sales.entity.*
-- ============================================================
--
-- 실행 방법
--   mysql -u <user> -p <database> < offline_tables.sql
--
-- 주의
--   1. production 에서는 spring.jpa.hibernate.ddl-auto=none 유지
--   2. offline_sales_sync_state.id 는 AUTO_INCREMENT 가 아님 (고정 PK 1)
--   3. 테이블 생성 순서: products → sales_infos → sales_items → item_discounts → payments → sync_state
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 오프라인 상품 (토스플레이스 카탈로그)
CREATE TABLE IF NOT EXISTS `offline_products` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `product_id`      VARCHAR(255)    NULL,
    `category_id`     VARCHAR(255)    NULL,
    `category_title`  VARCHAR(255)    NULL,
    `title`           VARCHAR(255)    NULL,
    `price_unit`      INT             NULL,
    `price_value`     INT             NULL,
    `barcode`         VARCHAR(255)    NULL,
    `created_at`      DATETIME        NULL,
    `updated_at`      DATETIME        NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_offline_products_product_id`
    ON `offline_products` (`product_id`);


-- 오프라인 판매 주문 헤더
CREATE TABLE IF NOT EXISTS `offline_sales_infos` (
    `id`                 BIGINT          NOT NULL AUTO_INCREMENT,
    `order_id`           VARCHAR(255)    NOT NULL,
    `order_state`        VARCHAR(255)    NULL,
    `order_number`       VARCHAR(255)    NULL,
    `created_at`         DATETIME        NULL,
    `list_price`         INT             NULL,
    `discount_amount`    INT             NULL,
    `tax_amount`         INT             NULL,
    `supply_amount`      INT             NULL,
    `tax_exempt_amount`  INT             NULL,
    `total_amount`       INT             NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_offline_sales_infos_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_offline_sales_infos_order_state_created_at`
    ON `offline_sales_infos` (`order_state`, `created_at`);

CREATE INDEX `idx_offline_sales_infos_created_at`
    ON `offline_sales_infos` (`created_at`);


-- 오프라인 판매 주문 라인 아이템
CREATE TABLE IF NOT EXISTS `offline_sales_items` (
    `id`                     BIGINT          NOT NULL AUTO_INCREMENT,
    `offline_sales_info_id`  BIGINT          NOT NULL,
    `line_item_id`           VARCHAR(255)    NOT NULL,
    `title`                  VARCHAR(255)    NULL,
    `category`               VARCHAR(255)    NULL,
    `price_unit`             INT             NULL,
    `price_value`            INT             NULL,
    `quantity`               INT             NULL,
    `memo`                   VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_offline_sales_items_line_item_id` (`line_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_offline_sales_items_offline_sales_info_id`
    ON `offline_sales_items` (`offline_sales_info_id`);


-- 오프라인 판매 라인 아이템 할인
CREATE TABLE IF NOT EXISTS `offline_sales_item_discounts` (
    `id`                     BIGINT          NOT NULL AUTO_INCREMENT,
    `offline_sales_item_id`  BIGINT          NOT NULL,
    `title`                  VARCHAR(255)    NULL,
    `amount`                 INT             NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_offline_sales_item_discounts_offline_sales_item_id`
    ON `offline_sales_item_discounts` (`offline_sales_item_id`);


-- 오프라인 판매 주문 결제 수단
CREATE TABLE IF NOT EXISTS `offline_sales_payments` (
    `id`                     BIGINT          NOT NULL AUTO_INCREMENT,
    `offline_sales_info_id`  BIGINT          NOT NULL,
    `payment_key`            VARCHAR(255)    NOT NULL,
    `source_type`            VARCHAR(255)    NULL,
    `amount`                 INT             NULL,
    `tax_amount`             INT             NULL,
    `supply_amount`          INT             NULL,
    `tax_exempt_amount`      INT             NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_offline_sales_payments_payment_key` (`payment_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_offline_sales_payments_offline_sales_info_id`
    ON `offline_sales_payments` (`offline_sales_info_id`);

CREATE INDEX `idx_offline_sales_payments_source_type`
    ON `offline_sales_payments` (`source_type`);


-- 오프라인 판매 동기화 상태 (단일 행, id=1 고정)
CREATE TABLE IF NOT EXISTS `offline_sales_sync_state` (
    `id`                  BIGINT          NOT NULL,
    `last_synced_at`      DATETIME        NULL,
    `last_success_at`     DATETIME        NULL,
    `last_error_message`  TEXT            NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
