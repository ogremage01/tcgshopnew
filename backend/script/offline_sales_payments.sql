-- 오프라인 판매 결제 수단 테이블 추가 (기존 DB 마이그레이션)
-- 실행: mysql -u <user> -p <database> < offline_sales_payments.sql

SET NAMES utf8mb4;

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
