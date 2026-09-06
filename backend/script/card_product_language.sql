CREATE TABLE IF NOT EXISTS `card_product_language` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(50) NOT NULL,
    `display_name` VARCHAR(100) NOT NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_card_product_language_code` (`code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
INSERT INTO `card_product_language` (
        `code`,
        `display_name`,
        `is_active`,
        `created_at`,
        `updated_at`
    )
VALUES ('en', 'ENGLISH', 1, NOW(), NOW()),
    ('ko', 'KOREAN', 1, NOW(), NOW()),
    ('ge', 'GERMAN', 1, NOW(), NOW()),
    ('sp', 'SPANISH', 1, NOW(), NOW()),
    ('fr', 'FRENCH', 1, NOW(), NOW()),
    ('it', 'ITALIAN', 1, NOW(), NOW()),
    ('ja', 'JAPANESE', 1, NOW(), NOW()),
    ('po', 'PORTUGUESE(BRAZIL)', 1, NOW(), NOW()),
    ('ru', 'RUSSIAN', 1, NOW(), NOW()),
    ('cs', 'CHINESESIMPLIFIED', 1, NOW(), NOW()),
    ('ct', 'CHINESE TRADITIONAL', 1, NOW(), NOW()) ON DUPLICATE KEY
UPDATE `display_name` =
VALUES(`display_name`),
    `is_active` =
VALUES(`is_active`),
    `updated_at` =
VALUES(`updated_at`);