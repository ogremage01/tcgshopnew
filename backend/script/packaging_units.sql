-- ============================================================
-- 포장 단위(PackagingUnit) 스키마 (PROD)
-- 대상 DB : MariaDB 10.x
-- 관련 엔티티: PackagingUnit
-- ============================================================
--
-- 실행 방법
--   mysql -u <user> -p <database> < packaging_units.sql
--
-- 주의
--   1. production 에서는 spring.jpa.hibernate.ddl-auto=validate(또는 none) 유지
--   2. 테이블이 이미 있으면 CREATE는 스킵됨
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `packaging_units` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `piece_id`      BIGINT NOT NULL COMMENT '낱개 OfflineProduct.id',
    `packaging_id`  BIGINT NOT NULL COMMENT '포장 OfflineProduct.id',
    `unit_count`    BIGINT NOT NULL COMMENT '포장 1개당 낱개 수',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_packaging_units_piece` (`piece_id`),
    UNIQUE KEY `uk_packaging_units_packaging` (`packaging_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
