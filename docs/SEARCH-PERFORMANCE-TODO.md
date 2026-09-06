# 검색 성능 — 미완 TODO (이슈 보류)

**상태:** 2026-05-19 기준 **여기서 종결·보류**. 다른 기능 우선.  
**관련:** [SEARCH-PERFORMANCE-IMPLEMENTATION-REPORT.md](./SEARCH-PERFORMANCE-IMPLEMENTATION-REPORT.md)

---

## 실측 요약 (보류 시점)

| 항목 | 값 |
|------|-----|
| 키워드 `searchInit` (서버) | **~1.3–1.7s** (`products` + `facets` 각 ~650–950ms) |
| 느린 SQL | `product_search_maps` 이름 `LIKE '%…%'` + `GROUP BY`; `union_prices` 서브쿼리(코드 경로) |
| EXPLAIN | `psm` ~9만 행 range 후 LIKE/OR/filesort; `union_prices` materialized ~18만 행(코드 `%…%` 시) |

**체감 목표(미달):** 키워드 `searchInit` 서버 **&lt;800ms** (이상적 **&lt;500ms**).

---

## 이미 한 일

- [x] `search perf` 로깅, dev `SQL_SLOW` (300ms), 로그 노이즈 정리 가이드
- [x] EXPLAIN으로 병목 특정 (이름 LIKE + 코드 OR + facet 동일 WHERE)
- [x] `check_code_refined` **전방 일치** (`startsWithIgnoreCase`) — [`ProductSearchMapRepositoryImpl`](../backend/src/main/java/com/shop/search/repository/ProductSearchMapRepositoryImpl.java)  
  - **카드명 검색**(`bird` 등)에는 효과 거의 없음 (예상됨)
  - 코드 검색도 `lower(컬럼)` 때문에 UK 활용 제한적일 수 있음

---

## TODO (재개 시 우선순위)

### P1 — MariaDB만 (ES 없이)

- [ ] **FULLTEXT:** `optional_fulltext_product_search_maps.sql` 적용 + QueryDSL `MATCH ... AGAINST` (`product_name`, `product_name_ko`)
- [ ] **코드 경로:** `lower()` 제거 검토(저장 케이스 통일 시 `startsWith` / `=`), 코드 검색 UX와 함께
- [ ] **`doSearchProducts`:** Phase 3 — 그룹 쿼리·N+1 조회 정리 (FULLTEXT 이후에도 400ms+ 이면)

### P2 — 선택

- [ ] **Redis facet 캐시:** `SEARCH_FACETS_CACHE_ENABLED=true` — **동일 키워드·필터 재요청**·인기어만, 첫 검색어 체감용 아님
- [ ] **검색 모드 UI:** “이름 / 코드” 분리(휴리스틱 자동 판별은 신뢰하기 어려움)

### P3 — 장기·인프라 검토

- [ ] **OpenSearch / Elasticsearch 도입 검토** — 학습·운영·동기화 비용 큼. 트래픽·데이터·팀 역량 보고 결정.  
  - 현 병목은 **단일 DB LIKE**로 특정되어 있어, **P1 미적용 상태에서 ES 도입은 우선순위 낮음**
- [ ] 운영 지속 측정: `searchInit` vs `searchProducts`, Slow query 상위 N

---

## 명시적 비범위 (이번 종결)

- 자동 EXPLAIN 로깅
- 키워드가 코드인지 이름인지 **코드만으로 완전 분기** (오탐·누락 불가피)

---

## 재개 시 첫 액션

1. `bird` 등으로 baseline `search perf` 재기록  
2. FULLTEXT DDL 수동 실행 → `buildProductSearchWhere` MATCH 분기  
3. 동일 조건 EXPLAIN·`search perf` 비교
