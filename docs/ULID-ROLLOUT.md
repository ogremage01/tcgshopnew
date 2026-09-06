# UnionPrice/ProductSearchMap ULID Rollout Runbook

## Scope
- External identifier standard: use ULID (`publicId`) for `UnionPrice` exposure.
- Internal join key remains `Long` (`id`, `sourceId`, `productId`) for now.
- `product_search_maps.source_public_id` is added as a forward-compatibility field.

## Code Behavior
- `UnionPrice` now guarantees `publicId` at insert time via `@PrePersist`.
- Union ingestion upsert writes `public_id` on insert, and only backfills it on duplicate update when blank/null.
- Scheduler flow includes `backfillMissingPublicIds()` after union ingestion to repair legacy rows.

## Backfill Policy
- Target rows: `union_prices.public_id IS NULL OR public_id = ''`.
- Batch strategy: application chunk processing (`1000` rows per chunk, id cursor based).
- Idempotency: rows with existing `public_id` are never overwritten.

## Verification Queries
```sql
-- 1) missing public id rows must be zero
SELECT COUNT(*) AS missing_public_id
FROM union_prices
WHERE public_id IS NULL OR public_id = '';

-- 2) duplicate public id rows must be zero
SELECT public_id, COUNT(*) AS cnt
FROM union_prices
WHERE public_id IS NOT NULL AND public_id <> ''
GROUP BY public_id
HAVING COUNT(*) > 1;

-- 3) ProductSearchMap sourcePublicId fill-rate check (UNION_PRICE only)
SELECT
  SUM(CASE WHEN source_public_id IS NULL OR source_public_id = '' THEN 1 ELSE 0 END) AS missing_source_public_id,
  COUNT(*) AS total
FROM product_search_maps
WHERE table_name = 'UNION_PRICE';
```

## Regression Checklist
- Product list/detail APIs still return valid `unionPriceId` values.
- Search filters (`rarity`, `setName`, `setCode`) and pagination behavior remain unchanged.
- `product_search_maps.sort_price` synchronization remains successful after ingestion.

## Compatibility Rules
- Keep `Long` PK/FK unchanged for query and join stability.
- Treat `tableId`, `searchMapId` as internal identifiers; do not use them in new public URL design.
- New external routes should prefer ULID path variables (`/{publicId}` pattern).
