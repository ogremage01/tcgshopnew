# 상품 검색 성능 개선 구현 보고서

본 문서는 **검색 초기 로딩 지연**에 대한 원인 분석 결론을 바탕으로 수행한 개선 작업과, 운영 시 참고할 설정·한계를 정리한 보고서입니다. (Redis 도입 여부는 “무조건”이 아니라 **선택적 캐시**로 반영.)

---

## 1. 목적과 범위

| 구분 | 내용 |
|------|------|
| 목적 | 검색 API의 **관측 가능성** 확보, **DB 라운드트립·벽시계 시간** 단축, 필요 시 **facet 전용 Redis 캐시** 활성화 |
| 포함 | `searchInit` / `searchProducts` 단계별 로깅, 개발 프로필 느린 SQL 로그, 인덱스·풀텍스트 참고 스크립트, facet 병렬 조회, SSR 병렬 fetch, 선택적 Spring Cache + Redis |
| 제외 | 키워드 검색을 FULLTEXT/MATCH로 전면 전환(QueryDSL 변경), 캐시 **명시적 무효화**(가격 동기화 이벤트 연동 등은 TTL 위주) |

---

## 2. 배경 요약

- `GET /api/products/search/init`는 **첫 페이지 상품 조회**와 **facet용 distinct 쿼리 다수**를 한 요청에서 처리하며, 키워드 조건 시 **이름 컬럼 LIKE** 및 **그룹/집계 쿼리** 부담이 클 수 있습니다.
- 프론트 SSR에서는 초기 진입이 아닌 경우 **init + 상품 목록 API**를 순차 호출하면 **체감 지연**이 커질 수 있어, 동시 요청으로 왕복 시간을 줄였습니다.

---

## 3. 구현 내용

### 3.1 관측·측정

| 항목 | 설명 |
|------|------|
| 서비스 로깅 | `ProductSearchMapServiceImpl`: `searchInit`에서 상품 단계·facet 단계 ms 분리, 합계 **500ms 이상 시 INFO**; `searchProducts` 동일 임계 |
| Hibernate | `application-dev.yml`: `LOG_QUERIES_SLOWER_THAN_MS: 300` 으로 느린 SQL 감지 |

### 3.2 DB·스키마

| 항목 | 설명 |
|------|------|
| 인덱스 | `ProductSearchMap` 엔티티에 `(is_visible, table_name)` 복합 인덱스 추가(`ddl-auto: update` 환경에서 생성) |
| 풀텍스트(참고) | `backend/src/main/resources/db/optional_fulltext_product_search_maps.sql` — **수동 실행**용 주석 예시. 앱은 여전히 LIKE 경로이므로 **쿼리 변경 전에는 효과 없음** |

### 3.3 라운드트립·병렬화

| 영역 | 설명 |
|------|------|
| 백엔드 | `CachedSearchFacetLoader`: facet distinct 5개 + UnionPrice 2개를 **전용 스레드 풀**에서 병렬 실행 |
| 프론트 | `ProductBrowseLayout.tsx`: `entryState` 등으로 init만 쓰지 않는 경우 **`Promise.all`로 init와 상품 목록 동시 요청** |

### 3.4 Redis 캐시(선택)

| 항목 | 설명 |
|------|------|
| 캐시 대상 | **facet 묶음만**(`SearchFacetsBundle`) — `Page`·`ProductItemDto` 직렬화 회피 |
| 비활성(기본) | `NoopSearchCacheManager` — 동작 변경 없음 |
| 활성 | `app.cache.search-facets.enabled=true` + Redis 연결, TTL 기본 **60초**(`SEARCH_FACETS_CACHE_TTL` 등으로 조정) |
| 무효화 | **TTL 위주**. 실시간성이 중요하면 TTL 축소 또는 동기화 지점에서 캐시 비우기 설계를 별도 검토 |

---

## 4. 설정 참고

### 4.1 애플리케이션

- `application.yml`: `app.cache.search-facets`, `spring.data.redis`
- 환경 변수 예: `SEARCH_FACETS_CACHE_ENABLED`, `SEARCH_FACETS_CACHE_TTL`, `REDIS_HOST`, `REDIS_PORT`

### 4.2 Docker Compose

- `docker-compose.yml`: `redis` 서비스, `backend`의 `depends_on: redis`, Redis 호스트·캐시 플래그 예시

### 4.3 로그 레벨

- 세부 단계: `logging.level.com.shop.search=DEBUG`

---

## 5. 주요 변경 파일 목록

| 경로 | 역할 |
|------|------|
| `backend/.../ProductSearchMapServiceImpl.java` | searchInit/searchProducts 성능 로깅, facet 로더 위임 |
| `backend/.../CachedSearchFacetLoader.java` | facet 병렬 + `@Cacheable` |
| `backend/.../SearchFacetExecutorConfig.java` | `searchFacetExecutor` 빈 |
| `backend/.../SearchFacetsBundle.java` | facet DTO(record) |
| `backend/.../SearchFacetsCacheKeyGenerator.java` | facet 캐시 키(필터 조합) |
| `backend/.../SearchCacheProperties.java` | `ConfigurationProperties` |
| `backend/.../NoopSearchCacheConfiguration.java` / `RedisSearchCacheConfiguration.java` | CacheManager 분기 |
| `backend/.../ShopApplication.java` | `@EnableCaching`, `@EnableConfigurationProperties` |
| `backend/.../ProductSearchMap.java` | 인덱스 |
| `backend/build.gradle.kts` | cache, data-redis |
| `backend/src/main/resources/application.yml` | cache/redis 기본값 |
| `backend/src/main/resources/application-dev.yml` | 느린 SQL |
| `backend/src/main/resources/db/optional_fulltext_product_search_maps.sql` | 풀텍스트 참고 |
| `frontend/.../ProductBrowseLayout.tsx` | SSR 병렬 호출 |
| `docker-compose.yml` | Redis·backend 환경 |

---

## 6. 향후 권장 과제

**2026-05-19:** 키워드 검색 체감 속도(~1.3–1.7s `searchInit`)는 **보류**. 상세·TODO·EXPLAIN 결론 → [**SEARCH-PERFORMANCE-TODO.md**](./SEARCH-PERFORMANCE-TODO.md)

1. **P1:** MariaDB FULLTEXT + QueryDSL (`product_name` / `product_name_ko`)
2. **P2:** Redis facet(재요청용), 검색 모드 UI(이름/코드)
3. **P3:** OpenSearch/Elasticsearch **도입 검토**(운영·학습 비용 대비, P1 이후 판단)

---

*문서 버전: 구현 완료 기준 정리 · TODO는 SEARCH-PERFORMANCE-TODO.md*
