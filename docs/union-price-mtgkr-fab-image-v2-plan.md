# FAB/MTG mtg-kr 가격 및 이미지 V2 전환

## Summary

개발 DB 초기화를 전제로 기존 카드 상품 연결까지 비우고 새 V2 흐름으로 재구성한다. 기존 구현은 레거시로 보존하고, FAB는 `fab_prices` mtg-kr 단일 소스로 `union_prices`를 만들며, MTG/FAB 이미지는 mtg-kr에서 다운로드해 내부 `/card-images/**`로 제공한다.

## Key Changes

- 기존 `UnionPriceIngestionServiceImpl`은 수정하지 않고 보존한다.
- 새 `UnionPriceIngestionServiceV2Impl`을 추가하고 `@Primary`로 등록한다.
- `tcg_p_sync_games`에서 FAB를 삭제해 TCGPlayer FAB 수집을 중단한다.
- FAB `imageUrl`은 `fabPrice.getSet() + "-" + fabPrice.getCode()`로 저장한다. 예: `fabtcgARCU-095-ENN`.
- FAB `setCode`는 `FabSetInfo` 기준의 표시 코드(`PEN`, `ARCU`)로 저장하고, `fab_prices` 원천 코드가 필요할 때만 백엔드 상수 `fabtcg`를 붙인다. 예: `FabSetInfo.PEN` -> `FabPrice.fabtcgPEN`.
- MTG `imageUrl`은 기존 `setCode + "-" + cardCode` 규칙을 유지한다.
- V2 UPSERT는 `image_source`, `image_url`도 업데이트한다.

## Image Download

- 새 `OpenBinderImageDownloadService`를 추가한다.
- 대상은 `union_prices.image_source = 'OPB'`이고 `image_url`이 있는 MTG/FAB 카드다.
- 원본 URL은 공개본에서 제외했다. 운영 환경의 외부 이미지 소스만 사용한다.
- 저장 경로는 `file.path.card-images/mtg-kr/{gameSlug}/{imageUrlLower}-en.png`로 둔다.
- 공개 URL은 `/card-images/mtg-kr/{gameSlug}/{imageUrlLower}-en.png`로 둔다.
- 양면/back 이미지 다운로드는 이번 범위에서 제외한다.

## Reset Runbook

개발 초기화 기준으로 `card_product`도 비운다.

실행 스크립트: `backend/script/union_price_mtgkr_v2_dev_reset.sql`

```sql
DELETE FROM tcg_p_sync_games
WHERE product_line_name = 'Flesh & Blood TCG'
   OR product_line_id = 62;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM product_search_maps
WHERE table_name IN ('UNION_PRICE', 'CARD_PRODUCT');

TRUNCATE TABLE card_product;
TRUNCATE TABLE union_prices;

SET FOREIGN_KEY_CHECKS = 1;
```

이후 `POST /api/admin/product/metadata/sync/openbinder-prices`로 가격을 재생성하고, `POST /api/admin/product/metadata/openbinder-images/download`로 OPB 이미지 다운로드를 실행한다.

## Test Plan

- FAB `set=fabtcgARCU`, `code=095-ENN`이 `imageUrl=fabtcgARCU-095-ENN`으로 저장되는지 확인한다.
- MTG/FAB 앞면 이미지가 `file.path.card-images/mtg-kr/...`에 저장되는지 확인한다.
- `/card-images/mtg-kr/...` URL로 이미지가 서빙되는지 확인한다.
- 초기화 후 FAB `union_prices`가 `fab_prices` 기준으로만 재생성되는지 확인한다.
