-- ============================================================
-- shop 데이터베이스 테이블 CREATE 스크립트
-- 대상 DB : MariaDB 10.x
-- 생성일  : 2026-06-08 (엔티티 기준 동기화)
-- ============================================================
--
-- 실행 방법
--   mysql -u <user> -p <database> < create_tables.sql
--
-- 주의 사항
--   1. production 환경에서는 application.yml의 spring.jpa.hibernate.ddl-auto를 none으로 설정한다.
--   2. 테이블 생성 전 데이터베이스가 존재해야 한다. (CREATE DATABASE shop CHARACTER SET utf8mb4 ...)
--   3. mtg_prices, fab_prices, tcg_p_prices 의 UNIQUE KEY는 UPSERT(INSERT ... ON DUPLICATE KEY UPDATE)
--      동작의 전제 조건이다. 초기 적재 전 중복 데이터가 없어야 한다.
--   4. union_prices.check_code_refined 는 UK(uk_check_code_refined)이며, 수동 정제 매칭 키로 쓰인다.
--   5. tcg_p_prices / mtg_prices / fab_prices 의 check_code_refined 는 배치·연동 로직에서 정제 키로 사용한다.
--   6. sync_log.proceeding_time 은 DurationNanosConverter에 의해 나노초 BIGINT로 저장된다.
--   7. product_search_maps.product_id 는 소스 테이블 public_id(ULID)를 담는다. (card_product.id 가 아님)
--
-- 인덱스 (JPA @Table 과 일치)
--   - tcg_p_prices: idx_tcg_p_prices_check_code, idx_tcg_p_prices_check_code_refined
--   - mtg_prices: idx_mtg_prices_check_code_refined
--   - fab_prices: idx_fab_prices_check_code_refined
--   - union_prices: uk_check_code_refined, idx_union_prices_public_id
--   - product_search_maps: table_name·source_id·catalog_source_id 등 6종 (엔티티 @Index)
-- 아래 tcg_p_price_id 인덱스는 엔티티에 없으나 Repository(findByTcgPPriceIdIsNull 등) 조회용으로 둔다.
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 사용자 & 인증
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    -- 외부 노출용 ULID. JWT subject·비관리 API 조회에 사용.
    `public_id`         VARCHAR(26)     NOT NULL,
    `name`              VARCHAR(255)    NOT NULL,
    -- 로그인 식별자. 소셜 로그인 포함 유일해야 한다.
    `email`             VARCHAR(255)    NOT NULL,
    -- 소셜 로그인(OAuth) 사용자는 NULL
    `password`          VARCHAR(100)    NULL,
    -- ROLE_USER | ROLE_ADMIN
    `role`              VARCHAR(20)     NOT NULL DEFAULT 'ROLE_USER',
    `point`             BIGINT          NOT NULL DEFAULT 0,
    `user_memo`         VARCHAR(200)    NULL     DEFAULT '',
    -- ACTIVE | INACTIVE | DELETED | BANNED
    `user_status`       VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    -- LOCAL | GOOGLE | KAKAO 등
    `auth_provider`     VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',
    `terms_agreed`      TINYINT(1)      NOT NULL DEFAULT 0,
    `terms_agreed_at`   DATETIME        NOT NULL,
    `terms_version`     VARCHAR(20)     NOT NULL DEFAULT '',
    `privacy_agreed`    TINYINT(1)      NOT NULL DEFAULT 0,
    `privacy_agreed_at` DATETIME        NOT NULL,
    `privacy_version`   VARCHAR(20)     NOT NULL DEFAULT '',
    `created_at`        DATETIME        NOT NULL,
    `updated_at`        DATETIME        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_users_email` (`email`),
    UNIQUE KEY `uq_users_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- JWT Refresh Token 관리
-- revoked_at IS NOT NULL 인 행은 폐기된 토큰이다.
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT          NOT NULL,
    -- SHA-256 해시값 (원본 토큰은 저장하지 않음)
    `token_hash`  VARCHAR(128)    NOT NULL,
    `expires_at`  DATETIME        NOT NULL,
    `revoked_at`  DATETIME        NULL,
    `created_at`  DATETIME        NOT NULL,
    `rotated_at`  DATETIME        NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_refresh_tokens_user_id`    ON `refresh_tokens` (`user_id`);
CREATE INDEX `idx_refresh_tokens_token_hash` ON `refresh_tokens` (`token_hash`);


-- ============================================================
-- 2. 상품 가격 (TCGPlayer / MTG / FAB)
-- ============================================================

CREATE TABLE IF NOT EXISTS `tcg_p_prices` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `product_id`      BIGINT          NULL,
    -- @Column(name = "card_condition")
    `card_condition`  VARCHAR(255)    NULL,
    `game`            VARCHAR(255)    NULL,
    `is_supplemental` TINYINT(1)      NULL,
    `market_price`    DECIMAL(19, 4)  NULL,
    `number`          VARCHAR(255)    NULL,
    `printing`        VARCHAR(255)    NULL,
    `product_name`    VARCHAR(255)    NULL,
    `rarity`          VARCHAR(255)    NULL,
    -- @Column(name = "set_name") (Java 필드명: set)
    `set_name`        VARCHAR(255)    NULL,
    `set_abbrv`       VARCHAR(255)    NULL,
    `type`            VARCHAR(255)    NULL,
    -- "Foil" | "Normal" | "Unknown"
    `print_type`      VARCHAR(50)     NULL,
    `is_double_sided` TINYINT(1)      NULL,
    -- 카드 이미지 다운로드 완료 여부
    `downloaded`      TINYINT(1)      NULL,
    -- "세트코드-카드번호" 형식
    `code_number`     VARCHAR(255)    NULL,
    -- mtg_prices / fab_prices 연결용 매칭 키
    `check_code`      VARCHAR(255)    NULL,
    -- 중복 검사 후 확정된 매칭 키(fab/mtg check_code_refined 와 비교)
    `check_code_refined` VARCHAR(255) NULL,
    -- Open Binder 행 PK (MTG: mtg_prices.id, FAB: fab_prices.id)
    `ob_price_id`     BIGINT          NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_tcg_p_prices_product_condition_printing`
        UNIQUE (`product_id`, `card_condition`, `printing`, `product_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_tcg_p_prices_product_id` ON `tcg_p_prices` (`product_id`);
CREATE INDEX `idx_tcg_p_prices_game`       ON `tcg_p_prices` (`game`);
CREATE INDEX `idx_tcg_p_prices_check_code` ON `tcg_p_prices` (`check_code`);
CREATE INDEX `idx_tcg_p_prices_check_code_refined` ON `tcg_p_prices` (`check_code_refined`);


CREATE TABLE IF NOT EXISTS `mtg_prices` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    -- @Column(name = "set_code") (Java 필드명: set)
    `set_code`       VARCHAR(50)     NULL,
    -- 카드 번호 (collector number)
    `code`           VARCHAR(50)     NULL,
    -- "normal" | "foil"
    `type`           VARCHAR(20)     NULL,
    `price`          DECIMAL(19, 4)  NULL,
    -- 외부 API(OpenBinder) 세트명·카드명·한글명
    `set_name`       VARCHAR(255)    NULL,
    `name`           VARCHAR(512)    NULL,
    `name_k`         VARCHAR(512)    NULL,
    `rarity`         VARCHAR(255)    NULL,
    `is_double_sided` TINYINT(1)     NULL,
    `layout`         VARCHAR(255)    NULL,
    -- OpenBinder 이미지 다운로드 완료 여부
    `downloaded`     TINYINT(1)      NULL,
    -- tcg_p_prices.id 와 연결 (PriceLinkService 에서 UPDATE JOIN으로 설정)
    `tcg_p_price_id` BIGINT          NULL,
    -- buildCheckCodeMtgForMtgPrice() 로 생성한 매칭 키 (set_code + code + type)
    `check_code`     VARCHAR(255)    NULL,
    -- 수동 정제 매칭 키
    `check_code_refined` VARCHAR(255) NULL,
    -- tcg_p_prices 의 condition-number-productName 조합
    `con_num_name`   VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_mtg_prices_set_code_type`
        UNIQUE (`set_code`, `code`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_mtg_prices_check_code_refined` ON `mtg_prices` (`check_code_refined`);
CREATE INDEX `idx_mtg_prices_tcg_p_price_id` ON `mtg_prices` (`tcg_p_price_id`);


CREATE TABLE IF NOT EXISTS `fab_prices` (
    `id`             BIGINT          NOT NULL AUTO_INCREMENT,
    -- @Column(name = "set_code") (Java 필드명: set)
    `set_code`       VARCHAR(50)     NULL,
    `code`           VARCHAR(100)    NULL,
    `price`          DECIMAL(19, 4)  NULL,
    `rarity`         VARCHAR(255)    NULL,
    `set_name`       VARCHAR(255)    NULL,
    `card_name`      VARCHAR(255)    NULL,
    `collector_num`  VARCHAR(50)     NULL,
    -- "Non-foil" | "Rainbow Foil" 등
    `foil`           VARCHAR(100)    NULL,
    -- OpenBinder 이미지 다운로드 완료 여부
    `downloaded`     TINYINT(1)      NULL,
    -- tcg_p_prices.id 와 연결 (PriceLinkService 에서 UPDATE JOIN으로 설정)
    `tcg_p_price_id` BIGINT          NULL,
    -- buildCheckCodeFabForFabPrice() 로 생성한 매칭 키
    `check_code`     VARCHAR(255)    NULL,
    -- 수동 정제 매칭 키
    `check_code_refined` VARCHAR(255) NULL,
    -- tcg_p_prices 의 condition-number-productName 조합
    `con_num_name`   VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_fab_prices_set_code`
        UNIQUE (`set_code`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_fab_prices_check_code_refined` ON `fab_prices` (`check_code_refined`);
CREATE INDEX `idx_fab_prices_tcg_p_price_id` ON `fab_prices` (`tcg_p_price_id`);


-- 통합 시세(배치 적재). CardProduct.union_price_id 가 참조한다 (FK 제약 없음).
CREATE TABLE IF NOT EXISTS `union_prices` (
    `id`                 BIGINT          NOT NULL AUTO_INCREMENT,
    -- 외부 노출용 ULID (@PrePersist 자동 생성)
    `public_id`          VARCHAR(26)     NOT NULL,
    `game`               VARCHAR(255)    NULL,
    `product_type`       VARCHAR(255)    NULL,
    `print_type`         VARCHAR(255)    NULL,
    `printing`           VARCHAR(255)    NULL,
    `price`              DECIMAL(19, 4)  NULL,
    `set_name`           VARCHAR(255)    NULL,
    `set_code`           VARCHAR(255)    NULL,
    `card_name`          VARCHAR(255)    NULL,
    `card_namek`         VARCHAR(255)    NULL,
    `rarity`             VARCHAR(255)    NULL,
    `check_code_refined` VARCHAR(255)    NULL,
    `image_source`       VARCHAR(255)    NULL,
    `image_url`          VARCHAR(255)    NULL,
    `is_double_sided`    TINYINT(1)      NULL,
    -- 세트넘버(숫자만)
    `set_number`         BIGINT          NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_check_code_refined` UNIQUE (`check_code_refined`),
    UNIQUE KEY `uq_union_prices_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_union_prices_public_id` ON `union_prices` (`public_id`);


-- ============================================================
-- 3. 카드 상품 & 가격 정책
-- ============================================================

CREATE TABLE IF NOT EXISTS `card_product` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT,
    -- 외부 노출용 ULID (26자 고정)
    `public_id`             VARCHAR(26)     NOT NULL,
    -- "Card" | "Sealed Product"
    `product_type`          VARCHAR(50)     NULL,
    -- @Column(name = "card_condition") : NM / EX / VG / G 등
    `card_condition`        VARCHAR(50)     NULL,
    -- "Foil" | "Normal"
    `print_type`            VARCHAR(50)     NULL,
    -- "Korean" | "English" 등
    `language`              VARCHAR(50)     NULL,
    `is_visible`            TINYINT(1)      NULL,
    `is_deleted`            TINYINT(1)      NULL,
    `current_visible_stock` BIGINT          NULL,
    `max_visible_stock`     BIGINT          NULL,
    `total_stock`           BIGINT          NULL,
    `is_auto_updated_stock` TINYINT(1)      NULL,
    -- storages.id 참조 (FK 없음)
    `storage_id`            BIGINT          NULL,
    -- union_prices.id 참조 (FK 없음)
    `union_price_id`        BIGINT          NULL,
    -- true: pricingRate를 시세에 곱해 가격 자동 계산
    `is_price_linked`       TINYINT(1)      NULL,
    `pricing_rate`          DOUBLE          NULL,
    -- 수동 설정 가격 (is_price_linked = false 일 때 사용)
    `price`                 BIGINT          NULL,
    -- 연동 계산된 가격
    `calculated_linked_price` BIGINT        NULL,
    `memo`                  VARCHAR(500)    NULL,
    `created_at`            DATETIME        NOT NULL,
    `updated_at`            DATETIME        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_card_product_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_card_product_is_deleted`   ON `card_product` (`is_deleted`);
CREATE INDEX `idx_card_product_union_price_id` ON `card_product` (`union_price_id`);


CREATE TABLE IF NOT EXISTS `card_product_language` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `code`            VARCHAR(50)  NOT NULL,
    `display_name`    VARCHAR(100) NOT NULL,
    `display_name_ko` VARCHAR(100) NOT NULL,
    `is_active`       TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`      DATETIME     NOT NULL,
    `updated_at`      DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_card_product_language_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `card_product_language` (`code`, `display_name`, `display_name_ko`, `is_active`, `created_at`, `updated_at`) VALUES
('en', 'English', '영어', 1, NOW(), NOW()),
('ko', 'Korean', '한글', 1, NOW(), NOW()),
('ge', 'German', '독일어', 1, NOW(), NOW()),
('sp', 'Spanish', '스페인어', 1, NOW(), NOW()),
('fr', 'French', '프랑스어', 1, NOW(), NOW()),
('it', 'Italian', '이탈리아어', 1, NOW(), NOW()),
('ja', 'Japanese', '일본어', 1, NOW(), NOW()),
('po', 'Portuguese(Brazil)', '포르투갈어(브라질)', 1, NOW(), NOW()),
('ru', 'Russian', '러시아어', 1, NOW(), NOW()),
('cs', 'ChineseSimplified', '중국어(간체)', 1, NOW(), NOW()),
('ct', 'Chinese Traditional', '중국어(번체)', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `display_name` = VALUES(`display_name`),
    `display_name_ko` = VALUES(`display_name_ko`),
    `is_active` = VALUES(`is_active`),
    `updated_at` = VALUES(`updated_at`);


CREATE TABLE IF NOT EXISTS `price_config` (
    -- ex) "exchange_rate", "min_price"
    `config_key`   VARCHAR(255)    NOT NULL,
    -- GameEnum.game 풀네임 (예: "Magic: The Gathering"). 빈 문자열은 전역값(현재 미사용).
    `config_game`  VARCHAR(100)    NOT NULL DEFAULT '',
    `config_value` DECIMAL(19, 4)  NOT NULL,
    `updated_at`   DATETIME        NOT NULL,
    PRIMARY KEY (`config_key`, `config_game`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `grade_pricing_policies` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `grade`      VARCHAR(20)     NOT NULL,
    `percentage` DOUBLE          NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `game_sales_infos` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `game`            VARCHAR(255)    NULL,
    `company`         VARCHAR(255)    NULL,
    `brand`           VARCHAR(255)    NULL,
    `origin`          VARCHAR(255)    NULL,
    `recommended_age` VARCHAR(255)    NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 4. 메타데이터 (TCGPlayer 동기화용)
-- ============================================================

CREATE TABLE IF NOT EXISTS `product_lines` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT,
    `product_line_id`       BIGINT          NULL,
    `product_line_name`     VARCHAR(255)    NULL,
    `product_line_url_name` VARCHAR(255)    NULL,
    `is_direct`             TINYINT(1)      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_product_lines_product_line_id` (`product_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `tcg_p_product_types` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `product_type_id` BIGINT          NULL,
    `product_name`    VARCHAR(255)    NULL,
    `product_line_id` BIGINT          NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_tcg_p_product_types_product_type_id` (`product_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `tcg_p_sync_games` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `product_line_id`   BIGINT          NULL,
    `product_line_name` VARCHAR(255)    NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_tcg_p_sync_games_product_line_id` (`product_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `set_names` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `set_name_id`     BIGINT          NULL,
    `category_id`     BIGINT          NULL,
    `name`            VARCHAR(255)    NULL,
    `clean_set_name`  VARCHAR(255)    NULL,
    `url_name`        VARCHAR(255)    NULL,
    `abbreviation`    VARCHAR(50)     NULL,
    `release_date`    DATETIME        NULL,
    `is_supplemental` TINYINT(1)      NULL,
    `active`          TINYINT(1)      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_set_names_set_name_id` (`set_name_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `set_name_maps` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `game`       VARCHAR(255)    NULL,
    `name`       VARCHAR(255)    NULL,
    `clean_name` VARCHAR(255)    NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `storages` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `storage_name` VARCHAR(255)    NULL,
    `description`  VARCHAR(255)    NULL,
    `is_default`   TINYINT(1)      NULL,
    `created_at`   DATETIME        NOT NULL,
    `updated_at`   DATETIME        NOT NULL,
    `is_deleted`   TINYINT(1)      NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mtg_set_infos` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `set_code`     VARCHAR(255)    NULL,
    `name`         VARCHAR(255)    NULL,
    `name_k`       VARCHAR(255)    NULL,
    `set_type`     VARCHAR(255)    NULL,
    `release_date` DATETIME        NULL,
    `created_at`   DATETIME        NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `fab_set_infos` (
    `id`       BIGINT          NOT NULL AUTO_INCREMENT,
    `set_code` VARCHAR(255)    NULL,
    `name`     VARCHAR(255)    NULL,
    `porder`   BIGINT          NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 5. 상품 검색 매핑
-- ============================================================

CREATE TABLE IF NOT EXISTS `product_search_maps` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `product_name`      VARCHAR(255)    NULL,
    `product_name_ko`   VARCHAR(255)    NULL,
    `game`              VARCHAR(255)    NULL,
    `set_code`          VARCHAR(64)     NULL,
    `product_type`      VARCHAR(255)    NULL,
    `supplies_type`     VARCHAR(255)    NULL,
    `manual_category`   VARCHAR(64)     NULL,
    `print_type`        VARCHAR(255)    NULL,
    `in_stock`          TINYINT(1)      NULL,
    -- 소스 테이블 public_id(ULID). SKU 단위 식별용.
    `product_id`        VARCHAR(26)     NOT NULL,
    `table_name`        VARCHAR(50)     NULL,
    `source_id`         BIGINT          NULL,
    `catalog_source_id` BIGINT          NULL,
    `source_public_id`  VARCHAR(26)     NULL,
    `sort_price`        DECIMAL(19, 4)  NULL,
    `is_visible`        TINYINT(1)      NULL,
    `updated_at`        DATETIME        NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_product_search_maps_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_product_search_maps_table_id_product_id` ON `product_search_maps` (`table_name`, `product_id`);
CREATE INDEX `idx_product_search_maps_table_name_source_id` ON `product_search_maps` (`table_name`, `source_id`);
CREATE INDEX `idx_product_search_maps_table_catalog_source_id` ON `product_search_maps` (`table_name`, `catalog_source_id`);
CREATE INDEX `idx_product_search_maps_table_name_source_public_id` ON `product_search_maps` (`table_name`, `source_public_id`);
CREATE INDEX `idx_product_search_maps_table_game_set_code` ON `product_search_maps` (`table_name`, `game`, `set_code`);
CREATE INDEX `idx_product_search_maps_visible_table` ON `product_search_maps` (`is_visible`, `table_name`);


-- ============================================================
-- 6. 장바구니 & 체크아웃
-- ============================================================

CREATE TABLE IF NOT EXISTS `carts` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT          NULL,
    `guest_id`   VARCHAR(255)    NULL,
    `created_at` DATETIME        NOT NULL,
    `updated_at` DATETIME        NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_carts_user_id`  ON `carts` (`user_id`);
CREATE INDEX `idx_carts_guest_id` ON `carts` (`guest_id`);


CREATE TABLE IF NOT EXISTS `cart_items` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `cart_id`       BIGINT          NOT NULL,
    `search_map_id` BIGINT          NOT NULL,
    `quantity`      BIGINT          NOT NULL,
    `updated_at`    DATETIME        NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_cart_items_cart_search_map` (`cart_id`, `search_map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_cart_items_cart_id` ON `cart_items` (`cart_id`);


CREATE TABLE IF NOT EXISTS `checkout_drafts` (
    `id`                          BIGINT          NOT NULL AUTO_INCREMENT,
    `public_id`                   VARCHAR(26)     NOT NULL,
    `user_id`                     BIGINT          NULL,
    `guest_id`                    VARCHAR(512)    NULL,
    `status`                      VARCHAR(32)     NOT NULL,
    `confirmed_order_id`          BIGINT          NULL,
    `delivery_method`             VARCHAR(32)     NULL,
    `recipient_name`              VARCHAR(255)    NULL,
    `recipient_address`           VARCHAR(500)    NULL,
    `recipient_phone`             VARCHAR(50)     NULL,
    `recipient_email`             VARCHAR(255)    NULL,
    `order_request`               VARCHAR(500)    NULL,
    `subtotal_amount`             DECIMAL(19, 4)  NOT NULL,
    `delivery_fee`                DECIMAL(19, 4)  NOT NULL,
    `used_point_amount`           DECIMAL(19, 4)  NOT NULL,
    `total_amount`                DECIMAL(19, 4)  NOT NULL,
    `expires_at`                  DATETIME        NOT NULL,
    `payment_currency`            VARCHAR(32)     NOT NULL,
    `payment_currency_rate`       DECIMAL(19, 4)  NOT NULL,
    `settle_krw_amount`           BIGINT          NOT NULL,
    `payment_method`              VARCHAR(32)     NULL,
    `total_product_amount`        DECIMAL(19, 4)  NULL,
    `created_at`                  DATETIME        NOT NULL,
    `updated_at`                  DATETIME        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_checkout_drafts_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_checkout_drafts_user_id`  ON `checkout_drafts` (`user_id`);
CREATE INDEX `idx_checkout_drafts_guest_id` ON `checkout_drafts` (`guest_id`);


CREATE TABLE IF NOT EXISTS `checkout_draft_items` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT,
    `draft_id`              BIGINT          NOT NULL,
    `cart_item_id`          BIGINT          NOT NULL,
    `cart_item_quantity`    BIGINT          NOT NULL,
    `cart_item_updated_at`  DATETIME        NULL,
    `search_map_id`         BIGINT          NOT NULL,
    `table_name`            VARCHAR(50)     NOT NULL,
    `source_id`             BIGINT          NOT NULL,
    `product_public_id`     VARCHAR(26)     NOT NULL,
    `product_type`          VARCHAR(64)     NOT NULL,
    `product_name_en`       VARCHAR(512)    NULL,
    `product_name_ko`       VARCHAR(512)    NULL,
    `image_url`             VARCHAR(1024)   NULL,
    `image_url_en`          VARCHAR(1024)   NULL,
    `image_url_ko`          VARCHAR(1024)   NULL,
    `snapshot_unit_price`   DECIMAL(19, 4)  NOT NULL,
    `quantity`              BIGINT          NOT NULL,
    `snapshot_total_price`  DECIMAL(19, 4)  NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_checkout_draft_items_draft_id` ON `checkout_draft_items` (`draft_id`);


-- ============================================================
-- 7. 주문
-- ============================================================

CREATE TABLE IF NOT EXISTS `order_configs` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `config_key`   VARCHAR(255)    NOT NULL,
    `config_value` VARCHAR(255)    NOT NULL,
    `is_enabled`   TINYINT(1)      NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `order_infos` (
    `id`                       BIGINT          NOT NULL AUTO_INCREMENT,
    `guest`                    TINYINT(1)      NOT NULL,
    `user_id`                  BIGINT          NULL,
    `recipient_name`           VARCHAR(255)    NULL,
    `recipient_address`        VARCHAR(1024)   NULL,
    `recipient_phone`          VARCHAR(512)    NULL,
    `recipient_email`          VARCHAR(512)    NULL,
    `order_request`            VARCHAR(500)    NULL,
    `order_status`             VARCHAR(50)     NULL,
    `payment_status`           VARCHAR(50)     NOT NULL,
    `delivery_company`         VARCHAR(100)    NULL,
    `delivery_tracking_number` VARCHAR(100)    NULL,
    `delivery_memo`            VARCHAR(500)    NULL,
    `payment_currency`         VARCHAR(20)     NULL,
    `total_product_amount`     DECIMAL(19, 4)  NULL,
    `total_payment_amount`     DECIMAL(19, 4)  NULL,
    `used_point_amount`        DECIMAL(19, 4)  NULL,
    `actual_payment_amount`    DECIMAL(19, 4)  NULL,
    `payment_date`             DATETIME        NULL,
    `delivery_fee`             DECIMAL(19, 4)  NULL,
    `guest_verification_code`  VARCHAR(16)     NULL,
    `pg_transaction_id`        VARCHAR(128)    NULL,
    `payment_approved_at`      DATETIME        NULL,
    `total_quantity`           BIGINT          NULL,
    `order_line_count`         BIGINT          NULL,
    `payment_method`           VARCHAR(255)    NULL,
    `payment_currency_rate_snapshot` DECIMAL(19, 4) NULL,
    `settle_krw_amount`        BIGINT          NULL,
    `earned_point_amount`      BIGINT          NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_order_infos_user_id` ON `order_infos` (`user_id`);


CREATE TABLE IF NOT EXISTS `order_products` (
    `id`                 BIGINT          NOT NULL AUTO_INCREMENT,
    `order_info_id`      BIGINT          NOT NULL,
    `product_id`         BIGINT          NOT NULL,
    `product_table`      VARCHAR(40)     NULL,
    `quantity`           BIGINT          NOT NULL,
    `price`              DECIMAL(19, 4)  NOT NULL,
    `total_price`        DECIMAL(19, 4)  NOT NULL,
    `search_map_id`      BIGINT          NULL,
    `product_public_id`  VARCHAR(26)     NULL,
    `product_name_ko`    VARCHAR(512)    NULL,
    `product_name_en`    VARCHAR(512)    NULL,
    `image_url`          VARCHAR(1024)   NULL,
    `product_type`       VARCHAR(64)     NULL,
    `snapshot_unit_price` BIGINT         NULL,
    `reward_points`      BIGINT          NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_order_products_order_info_id` ON `order_products` (`order_info_id`);


-- ============================================================
-- 8. 소모품 & 기타 상품
-- ============================================================

CREATE TABLE IF NOT EXISTS `makers` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(255)    NOT NULL,
    `is_deleted` TINYINT(1)      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_makers_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `supply_types` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `name_en`    VARCHAR(255)    NOT NULL,
    `name_ko`    VARCHAR(255)    NOT NULL,
    `is_deleted` TINYINT(1)      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_supply_types_name_en` (`name_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `supplies` (
    `id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `public_id`    VARCHAR(26)     NOT NULL,
    `name_en`      VARCHAR(255)    NULL,
    `name_ko`      VARCHAR(255)    NULL,
    `description`  TEXT            NULL,
    `price`        BIGINT          NULL,
    `stock`        BIGINT          NULL,
    `supply_type`  VARCHAR(255)    NULL,
    `maker`        VARCHAR(255)    NULL,
    `is_visible`   TINYINT(1)      NULL,
    `is_deleted`   TINYINT(1)      NULL,
    `img_url`      VARCHAR(255)    NULL,
    `created_at`   DATETIME        NOT NULL,
    `updated_at`   DATETIME        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_supplies_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `product_category` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `name_en`    VARCHAR(255)    NOT NULL,
    `name_ko`    VARCHAR(255)    NOT NULL,
    `is_deleted` TINYINT(1)      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_product_category_name_en` (`name_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `product_ip` (
    `id`         BIGINT          NOT NULL AUTO_INCREMENT,
    `name_en`    VARCHAR(255)    NOT NULL,
    `name_ko`    VARCHAR(255)    NOT NULL,
    `is_deleted` TINYINT(1)      NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_product_ip_name_en` (`name_en`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `sealed_product` (
    `id`                   BIGINT          NOT NULL AUTO_INCREMENT,
    `product_name_en`      VARCHAR(255)    NULL,
    `product_name_ko`      VARCHAR(255)    NULL,
    `description`          VARCHAR(255)    NULL,
    `image_url`            VARCHAR(255)    NULL,
    `price`                BIGINT          NULL,
    `current_visible_stock` INT             NULL,
    `max_visible_stock`    INT             NULL,
    `total_stock`          INT             NULL,
    `is_active`            TINYINT(1)      NULL,
    `is_deleted`           TINYINT(1)      NULL,
    `game`                 VARCHAR(255)    NULL,
    `set_name`             VARCHAR(255)    NULL,
    `set_code`             VARCHAR(255)    NULL,
    `language`             VARCHAR(255)    NULL,
    `public_id`            VARCHAR(26)     NOT NULL,
    `offline_product_id`   VARCHAR(255)    NULL,
    `created_at`           DATETIME        NULL,
    `updated_at`           DATETIME        NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_sealed_product_public_id` (`public_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `manual_products` (
    `id`                 BIGINT          NOT NULL AUTO_INCREMENT,
    `name_en`            VARCHAR(255)    NULL,
    `name_ko`            VARCHAR(255)    NULL,
    `description`        TEXT            NULL,
    `price`              BIGINT          NULL,
    `stock`              BIGINT          NULL,
    `product_type`       VARCHAR(255)    NULL,
    `product_ip`         VARCHAR(255)    NULL,
    `img_url`            VARCHAR(255)    NULL,
    `is_deleted`         TINYINT(1)      NULL,
    `is_visible`         TINYINT(1)      NULL,
    `public_id`          VARCHAR(255)    NULL,
    `offline_product_id` VARCHAR(255)    NULL,
    `created_at`         DATETIME        NOT NULL,
    `updated_at`         DATETIME        NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 9. 사이트 설정 & 적립
-- ============================================================

CREATE TABLE IF NOT EXISTS `banners` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `target`        VARCHAR(100)    NULL,
    `image_url`     VARCHAR(500)    NULL,
    `link`          VARCHAR(500)    NULL,
    `title`         VARCHAR(255)    NULL,
    `display_order` INT             NULL,
    `active`        TINYINT(1)      NULL,
    `created_at`    DATETIME        NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `set_banners` (
    `game`              VARCHAR(50)     NOT NULL,
    `banner_id`         VARCHAR(100)    NOT NULL,
    `banner_image_url`  VARCHAR(500)    NULL,
    `banner_link`       VARCHAR(500)    NULL,
    `banner_title`      VARCHAR(255)    NULL,
    `banner_active`     TINYINT(1)      NULL,
    `created_at`        DATETIME        NOT NULL,
    `updated_at`        DATETIME        NOT NULL,
    PRIMARY KEY (`game`, `banner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `main_page_contents` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `name`          VARCHAR(255)    NULL,
    `content`       TEXT            NULL,
    `image_url`     VARCHAR(255)    NULL,
    `link`          VARCHAR(255)    NULL,
    `display_order` INT             NULL,
    `active`        TINYINT(1)      NULL,
    `created_at`    DATETIME        NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `main_header` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `title`         VARCHAR(255)    NOT NULL,
    `url_string`    VARCHAR(1000)   NOT NULL,
    `is_active`     TINYINT(1)      NOT NULL,
    `display_order` INT             NOT NULL,
    `created_at`    DATETIME        NOT NULL,
    `updated_at`    DATETIME        NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `reward_rules` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `name`              VARCHAR(255)    NOT NULL,
    `rank`              INT             NULL,
    `reward_percentage` DOUBLE          NULL,
    `cal_rule`          JSON            NULL,
    `is_active`         TINYINT(1)      NOT NULL,
    `start_at`          DATETIME        NULL,
    `end_at`            DATETIME        NULL,
    `created_at`        DATETIME        NOT NULL,
    `updated_at`        DATETIME        NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_reward_rules_rank` (`rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS `mails` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `mail_to`     VARCHAR(255)    NOT NULL,
    `is_verified` TINYINT(1)      NOT NULL,
    `created_at`  DATETIME        NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
-- 10. 로그
-- ============================================================

CREATE TABLE IF NOT EXISTS `order_log` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT          NULL,
    `category`    VARCHAR(100)    NULL,
    `action`      VARCHAR(255)    NULL,
    `action_date` DATETIME        NULL,
    `user_id`     BIGINT          NULL,
    `user_name`   VARCHAR(100)    NULL,
    `is_success`  TINYINT(1)      NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_order_log_order_id` ON `order_log` (`order_id`);


CREATE TABLE IF NOT EXISTS `user_log` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `category`    VARCHAR(100)    NULL,
    `user_id`     BIGINT          NULL,
    `action`      VARCHAR(255)    NULL,
    `action_date` DATETIME        NULL,
    `user_name`   VARCHAR(100)    NULL,
    `executor`    VARCHAR(255)    NULL,
    `is_success`  TINYINT(1)      NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_user_log_user_id` ON `user_log` (`user_id`);


CREATE TABLE IF NOT EXISTS `point_log` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT          NULL,
    `user_name`     VARCHAR(255)    NULL,
    `changed_point` BIGINT          NULL,
    `before_point`  BIGINT          NULL,
    `after_point`   BIGINT          NULL,
    `change_reason` VARCHAR(255)    NULL,
    `actor_type`    VARCHAR(255)    NULL,
    `executor`      VARCHAR(255)    NULL,
    `action_date`   DATETIME        NULL,
    `is_success`    TINYINT(1)      NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_point_log_user_id` ON `point_log` (`user_id`);


CREATE TABLE IF NOT EXISTS `sync_log` (
    `id`               BIGINT          NOT NULL AUTO_INCREMENT,
    `sync_source`      VARCHAR(255)    NULL,
    `sync_target`      VARCHAR(255)    NULL,
    `start_time`       DATETIME        NULL,
    `end_time`         DATETIME        NULL,
    -- Duration 나노초 저장 (DurationNanosConverter)
    `proceeding_time`  BIGINT          NULL COMMENT '나노초 단위. DurationNanosConverter 참조.',
    `result`           VARCHAR(50)     NULL,
    `message`          TEXT            NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_sync_log_sync_source` ON `sync_log` (`sync_source`);
CREATE INDEX `idx_sync_log_start_time`  ON `sync_log` (`start_time`);


CREATE TABLE IF NOT EXISTS `mail_log` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `mail_to`     VARCHAR(255)    NULL,
    `category`    VARCHAR(100)    NULL,
    `created_at`  DATETIME        NULL,
    `is_success`  TINYINT(1)      NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;
