-- fab_prices.foil: Non-Foil 계열 → Normal (1회)
-- check_code / check_code_refined 는 FAB OpenBinder 재동기화 후 자동 재생성된다.

UPDATE fab_prices
SET foil = 'Normal'
WHERE foil IS NOT NULL
  AND (
        UPPER(TRIM(foil)) LIKE '%NON-FOIL%'
        OR UPPER(TRIM(foil)) LIKE '%NON FOIL%'
      )
  AND foil <> 'Normal';

-- 적용 후 foil 분포 확인
SELECT foil, COUNT(*) AS cnt
FROM fab_prices
GROUP BY foil
ORDER BY cnt DESC;
