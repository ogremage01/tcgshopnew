-- union_prices.printing → Foil·Normal 이진값 백필 (1회)
-- print_type 은 절대 수정하지 않음. printing 만 갱신한다.
-- 판별: print_type(Foil/Normal) 우선 → printing 원문 → ELSE Normal

-- ---------------------------------------------------------------------------
-- 1) 적용 전 미리보기 (선택)
-- ---------------------------------------------------------------------------
SELECT
    id,
    game,
    print_type,
    printing,
    CASE
        WHEN LOWER(TRIM(COALESCE(print_type, ''))) = 'foil' THEN 'Foil'
        WHEN LOWER(TRIM(COALESCE(print_type, ''))) = 'normal' THEN 'Normal'
        WHEN LOWER(TRIM(COALESCE(print_type, ''))) IN ('non-foil', 'non foil') THEN 'Normal'
        WHEN UPPER(
            REPLACE(
                REPLACE(
                    REPLACE(COALESCE(printing, ''), 'Non-Foil', 'Normal'),
                    'Non-foil', 'Normal'
                ),
                'non-foil', 'Normal'
            )
        ) LIKE '%FOIL%' THEN 'Foil'
        WHEN UPPER(COALESCE(printing, '')) LIKE '%NORMAL%'
            OR LOWER(TRIM(COALESCE(printing, ''))) IN ('non-foil', 'non foil', 'normal') THEN 'Normal'
        WHEN LOWER(TRIM(COALESCE(printing, ''))) = 'etched' THEN 'Foil'
        ELSE 'Normal'
    END AS resolved_printing
FROM union_prices
WHERE printing IS NOT NULL
  AND printing NOT IN ('Foil', 'Normal')
ORDER BY id
LIMIT 200;

-- ---------------------------------------------------------------------------
-- 2) 백필 UPDATE (printing 만)
-- ---------------------------------------------------------------------------
UPDATE union_prices
SET printing = CASE
        WHEN LOWER(TRIM(COALESCE(print_type, ''))) = 'foil' THEN 'Foil'
        WHEN LOWER(TRIM(COALESCE(print_type, ''))) = 'normal' THEN 'Normal'
        WHEN LOWER(TRIM(COALESCE(print_type, ''))) IN ('non-foil', 'non foil') THEN 'Normal'
        WHEN UPPER(
            REPLACE(
                REPLACE(
                    REPLACE(COALESCE(printing, ''), 'Non-Foil', 'Normal'),
                    'Non-foil', 'Normal'
                ),
                'non-foil', 'Normal'
            )
        ) LIKE '%FOIL%' THEN 'Foil'
        WHEN UPPER(COALESCE(printing, '')) LIKE '%NORMAL%'
            OR LOWER(TRIM(COALESCE(printing, ''))) IN ('non-foil', 'non foil', 'normal') THEN 'Normal'
        WHEN LOWER(TRIM(COALESCE(printing, ''))) = 'etched' THEN 'Foil'
        ELSE 'Normal'
    END
WHERE printing IS NOT NULL
  AND printing NOT IN ('Foil', 'Normal');

-- ---------------------------------------------------------------------------
-- 3) 적용 후 검증 (선택)
-- ---------------------------------------------------------------------------
SELECT printing, print_type, COUNT(*) AS cnt
FROM union_prices
GROUP BY printing, print_type
ORDER BY cnt DESC;

SELECT COUNT(*) AS printing_not_binary
FROM union_prices
WHERE printing NOT IN ('Foil', 'Normal');

SELECT COUNT(*) AS print_type_printing_mismatch
FROM union_prices
WHERE print_type IN ('Foil', 'Normal')
  AND printing IN ('Foil', 'Normal')
  AND print_type <> printing;
