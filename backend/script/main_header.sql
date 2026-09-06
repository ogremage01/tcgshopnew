-- main_header: 스토어프론트 헤더 네비 단일 항목(extras). prod는 ddl-auto validate/none 유지 후 수동 실행.
-- Dev/demo(ddl-auto=update)에서는 엔티티로 테이블이 생길 수 있으나, 시드 INSERT는 이 스크립트 또는 관리자 UI로 등록.

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

-- 초기 시드 (이미 동일 title이 있으면 건너뛰려면 관리자 UI로 등록)
INSERT INTO `main_header` (`title`, `url_string`, `is_active`, `display_order`, `created_at`, `updated_at`)
SELECT 'Pre-order',
       '/special/products?manualCategories=Preorder%20Products&page=0&entryState=UPDATED',
       1,
       1,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM `main_header` WHERE `title` = 'Pre-order');

INSERT INTO `main_header` (`title`, `url_string`, `is_active`, `display_order`, `created_at`, `updated_at`)
SELECT 'Event Ticket',
       '/special/products?manualCategories=Event%20Ticket&page=0&entryState=UPDATED',
       1,
       2,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM `main_header` WHERE `title` = 'Event Ticket');
