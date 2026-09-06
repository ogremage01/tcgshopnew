-- ProductCategory / ProductIp 마스터 도입
-- Manual: product_type / product_ip 에 마스터 nameEn 문자열 저장 (Supply와 동일 비정규화)
-- 폼에서는 categoryId / productIpId 로 선택 → 서버가 nameEn 저장
-- Sealed: game 문자열만
-- 실행 전 백업 권장.

-- ============================================================
-- 1. 마스터 테이블
-- ============================================================

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

-- ============================================================
-- 2. 시드 (기존 하드코딩 Select 값)
-- ============================================================

INSERT INTO product_category (name_en, name_ko, is_deleted)
SELECT v.name_en, v.name_ko, 0
FROM (
    SELECT 'preorder' AS name_en, '프리오더' AS name_ko
    UNION ALL SELECT 'event-ticket', '이벤트 참가권'
    UNION ALL SELECT 'etc', '기타'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM product_category c WHERE c.name_en = v.name_en
);

INSERT INTO product_ip (name_en, name_ko, is_deleted)
SELECT v.name_en, v.name_ko, 0
FROM (
    SELECT 'Magic: The Gathering' AS name_en, 'MTG' AS name_ko
    UNION ALL SELECT 'Flesh & Blood TCG', 'FAB'
    UNION ALL SELECT 'Lorcana TCG', 'LOR'
    UNION ALL SELECT 'Riftbound League of Legends Trading Card Game', 'RIFT'
    UNION ALL SELECT 'Star Wars Unlimited', 'STARWARS'
    UNION ALL SELECT 'Disney Lorcana', 'LOR'
    UNION ALL SELECT 'Riftbound: League of Legends Trading Card Game', 'RIFT'
    UNION ALL SELECT 'Star Wars: Unlimited', 'STARWARS'
    UNION ALL SELECT 'other', 'OTHER'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM product_ip p WHERE p.name_en = v.name_en
);

-- ============================================================
-- 3. (이전에 category_id / product_ip_id FK를 넣었다면 이름 복원 후 컬럼 제거)
-- ============================================================
-- UPDATE manual_products mp
-- LEFT JOIN product_category pc ON pc.id = mp.category_id
-- SET mp.product_type = COALESCE(pc.name_en, mp.product_type)
-- WHERE mp.category_id IS NOT NULL;
--
-- UPDATE manual_products mp
-- LEFT JOIN product_ip pi ON pi.id = mp.product_ip_id
-- SET mp.product_ip = COALESCE(pi.name_en, mp.product_ip)
-- WHERE mp.product_ip_id IS NOT NULL;
--
-- ALTER TABLE manual_products DROP COLUMN category_id;
-- ALTER TABLE manual_products DROP COLUMN product_ip_id;
-- ALTER TABLE sealed_product DROP COLUMN product_ip_id;

-- ============================================================
-- 4. 검색맵 백필
-- ============================================================

UPDATE product_search_maps psm
INNER JOIN manual_products mp
    ON psm.source_id = mp.id AND psm.table_name = 'MANUAL_PRODUCT'
SET
    psm.manual_category = mp.product_type,
    psm.game = mp.product_ip,
    psm.product_type = 'ManualProducts'
WHERE psm.table_name = 'MANUAL_PRODUCT';
