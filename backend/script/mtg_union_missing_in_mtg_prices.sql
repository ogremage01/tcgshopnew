-- MTG union_prices ↔ mtg_prices 누락 항목 진단
-- union에는 check_code_refined가 있지만 mtg_prices에 매칭 행이 없는 OPB Cards 항목을 찾는다.
-- 조인 키: check_code_refined (union_prices_print_type_printing_backfill.sql 과 동일)

-- ---------------------------------------------------------------------------
-- 1) 건수 확인
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS missing_cnt
FROM union_prices u
WHERE u.game = 'Magic: The Gathering'
  AND u.image_source = 'OPB'
  AND u.product_type = 'Cards'
  AND u.check_code_refined IS NOT NULL
  AND TRIM(u.check_code_refined) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM mtg_prices m
      WHERE m.check_code_refined = u.check_code_refined
  );

-- ---------------------------------------------------------------------------
-- 2) 상세 목록 (필터 리셋 전 목록 복원용)
-- ---------------------------------------------------------------------------
SELECT
    u.id,
    u.public_id,
    u.check_code_refined,
    u.set_code,
    u.set_name,
    u.card_name,
    u.card_namek,
    u.print_type,
    u.printing,
    u.price,
    u.image_url,
    u.rarity,
    u.set_number
FROM union_prices u
WHERE u.game = 'Magic: The Gathering'
  AND u.image_source = 'OPB'
  AND u.product_type = 'Cards'
  AND u.check_code_refined IS NOT NULL
  AND TRIM(u.check_code_refined) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM mtg_prices m
      WHERE m.check_code_refined = u.check_code_refined
  )
ORDER BY u.set_code, u.set_number, u.check_code_refined;

-- ---------------------------------------------------------------------------
-- 3) 보조 진단 — DUPLICATE_CODE로 mtg에만 있는 경우
-- union에는 정상 키가 있는데 mtg는 DUPLICATE_CODE로만 남은 경우를 추가로 본다.
-- ---------------------------------------------------------------------------
SELECT
    u.id,
    u.check_code_refined,
    u.set_code,
    u.card_name,
    m_dup.cnt AS mtg_duplicate_code_rows
FROM union_prices u
LEFT JOIN (
    SELECT check_code_refined, COUNT(*) AS cnt
    FROM mtg_prices
    WHERE check_code_refined = 'DUPLICATE_CODE'
    GROUP BY check_code_refined
) m_dup ON 1 = 1
WHERE u.game = 'Magic: The Gathering'
  AND u.image_source = 'OPB'
  AND u.product_type = 'Cards'
  AND u.check_code_refined IS NOT NULL
  AND TRIM(u.check_code_refined) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM mtg_prices m
      WHERE m.check_code_refined = u.check_code_refined
  )
ORDER BY u.set_code, u.check_code_refined
LIMIT 200;

-- ---------------------------------------------------------------------------
-- 4) 역방향 sanity check (선택)
-- mtg에는 있는데 union OPB Cards에는 없는 키
-- ---------------------------------------------------------------------------
SELECT COUNT(*) AS mtg_only_cnt
FROM mtg_prices m
WHERE m.check_code_refined IS NOT NULL
  AND TRIM(m.check_code_refined) <> ''
  AND m.check_code_refined <> 'DUPLICATE_CODE'
  AND NOT EXISTS (
      SELECT 1
      FROM union_prices u
      WHERE u.check_code_refined = m.check_code_refined
        AND u.game = 'Magic: The Gathering'
        AND u.image_source = 'OPB'
        AND u.product_type = 'Cards'
  );
