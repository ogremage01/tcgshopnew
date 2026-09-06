package com.shop.search.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.card.entity.UnionPrice;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.enums.GameEnum;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.dto.searching.ProductSearchingDto;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.helper.ManualCategoryAliases;
import com.shop.search.helper.QueryDslFilterHelper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductSearchMapRepositoryImpl implements ProductSearchMapRepositoryCustom {

    private static final String PROP_SORT_PRICE = "sortPrice";
    private static final String PROP_PRODUCT_NAME = "productName";
    private static final String PROP_SET_NUMBER = "setNumber";
    private static final String PROP_ID = "id";

    private final JPAQueryFactory queryFactory;

    /**
     * {@link ProductSearchMap} / 서브쿼리용 {@link UnionPrice} QueryDSL 경로 묶음.
     * 검색 목록·facet이 동일 alias·컬럼 경로를 쓰도록 한곳에서 생성한다.
     */
    private static final class SearchPaths {

        final PathBuilder<ProductSearchMap> map;
        final NumberPath<Long> id;
        final StringPath productName;
        final StringPath productNameKo;
        final StringPath game;
        final StringPath productType;
        final StringPath suppliesType;
        final StringPath manualCategory;
        final StringPath printType;
        final BooleanPath inStock;
        final BooleanPath isVisible;
        final NumberPath<java.math.BigDecimal> sortPrice;
        final NumberPath<Long> sourceId;
        final EnumPath<ProductTableEnum> tableName;
        final StringPath setCodeColumn;
        final PathBuilder<UnionPrice> unionPrice;
        final NumberPath<Long> unionPriceId;
        final StringPath rarity;
        final StringPath setName;
        final StringPath unionSetCode;
        final StringPath unionCheckCodeRefined;
        final NumberPath<Long> unionSetNumber;
        final PathBuilder<SealedProduct> sealedProduct;
        final NumberPath<Long> sealedProductId;
        final StringPath sealedSetName;

        private SearchPaths() {
            this.map = new PathBuilder<>(ProductSearchMap.class, "productSearchMap");
            this.id = map.getNumber(PROP_ID, Long.class);
            this.productName = map.getString(PROP_PRODUCT_NAME);
            this.productNameKo = map.getString("productNameKo");
            this.game = map.getString("game");
            this.productType = map.getString("productType");
            this.suppliesType = map.getString("suppliesType");
            this.manualCategory = map.getString("manualCategory");
            this.printType = map.getString("printType");
            this.inStock = map.getBoolean("inStock");
            this.isVisible = map.getBoolean("isVisible");
            this.sortPrice = map.getNumber(PROP_SORT_PRICE, java.math.BigDecimal.class);
            this.sourceId = map.getNumber("sourceId", Long.class);
            this.tableName = map.getEnum("tableName", ProductTableEnum.class);
            this.setCodeColumn = map.getString("setCode");
            this.unionPrice = new PathBuilder<>(UnionPrice.class, "unionPrice");
            this.unionPriceId = unionPrice.getNumber("id", Long.class);
            this.rarity = unionPrice.getString("rarity");
            this.setName = unionPrice.getString("setName");
            this.unionSetCode = unionPrice.getString("setCode");
            this.unionCheckCodeRefined = unionPrice.getString("checkCodeRefined");
            this.unionSetNumber = unionPrice.getNumber(PROP_SET_NUMBER, Long.class);
            this.sealedProduct = new PathBuilder<>(SealedProduct.class, "sealedProduct");
            this.sealedProductId = sealedProduct.getNumber("id", Long.class);
            this.sealedSetName = sealedProduct.getString("setName");
        }

        static SearchPaths create() {
            return new SearchPaths();
        }
    }

    /**
     * 상품 검색·facet 집계에 공통으로 쓰는 WHERE 절을 만든다.
     * <p>
     * 단계 요약: (0) 노출 행만 isVisible=true,
     * (1) 키워드 → 이름/한글명 부분일치 또는 체크코드 전방일치(prefix),
     * (2) 맵에 붙는 필터(게임·세트코드·타입·포일·재고),
     * (3) 레어/세트명/setCode가 있으면 UnionPrice 서브쿼리로 sourceId 제한.
     */
    private BooleanBuilder buildProductSearchWhere(ProductSearchingDto query, SearchPaths p) {
        BooleanBuilder where = new BooleanBuilder();

        // 0) 고객 검색·facet 공통: 비노출 행 제외 (기존 JPQL facet의 isVisible = true 와 동일)
        where.and(p.isVisible.eq(true));
        where.and(p.tableName.ne(ProductTableEnum.CARD_PRODUCT));
        // 임시: Lorcana는 헤더와 같이 고객 검색에서 제외 (언락 시 GameEnum 목록에서 제거)
        where.and(p.game.isNull()
                .or(p.game.notIn(GameEnum.temporarilyHiddenFromCustomerSearchGameNames())));

        // 1) 키워드: 상품명·한글명은 부분일치, check_code_refined는 UK 인덱스 활용을 위해 전방일치만
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String k = query.getKeyword().trim();
            BooleanExpression nameMatch = p.productName.containsIgnoreCase(k)
                    .or(p.productNameKo.containsIgnoreCase(k));
            BooleanExpression codeMatch = p.tableName.eq(ProductTableEnum.UNION_PRICE)
                    .and(p.sourceId.in(
                            JPAExpressions.select(p.unionPriceId)
                                    .from(p.unionPrice)
                                    .where(p.unionCheckCodeRefined.startsWithIgnoreCase(k))));
            where.and(nameMatch.or(codeMatch));
        }

        // 2) ProductSearchMap 컬럼에 직접 걸리는 필터들(게임·세트코드·타입·포일·재고)
        where.and(QueryDslFilterHelper.inIfNotEmpty(
                GameEnum.expandSearchNames(query.getGames()), p.game));
        if (query.getSetCode() != null && !query.getSetCode().isBlank()) {
            where.and(p.setCodeColumn.eq(query.getSetCode().trim()));
        }
        where.and(QueryDslFilterHelper.inIfNotEmpty(query.getProductTypes(), p.productType));
        where.and(QueryDslFilterHelper.inIfNotEmpty(query.getSuppliesTypes(), p.suppliesType));
        where.and(QueryDslFilterHelper.inIfNotEmpty(
                ManualCategoryAliases.expand(query.getManualCategories()), p.manualCategory));
        where.and(QueryDslFilterHelper.printTypeIfFoilKnown(query.getIsFoil(), p.printType));
        where.and(QueryDslFilterHelper.eqIfNotNull(query.getIsInStock(), p.inStock));

        // 3) 레어·세트명: 카드(UnionPrice) 및 sealed(SealedProduct.setName) 축으로 좁힘
        // setCode가 있으면 setNames(세트명 리스트)는 무시 — 검색 본쿼리와 동일 규칙
        boolean hasSetCode = query.getSetCode() != null && !query.getSetCode().isBlank();
        boolean hasRarities = query.getRarities() != null && !query.getRarities().isEmpty();
        boolean hasSetNames = !hasSetCode && query.getSetNames() != null && !query.getSetNames().isEmpty();
        if (hasRarities || hasSetNames) {
            BooleanBuilder productTypeMatch = new BooleanBuilder();

            BooleanBuilder unionPriceWhere = new BooleanBuilder();
            unionPriceWhere.and(QueryDslFilterHelper.inIfNotEmpty(query.getRarities(), p.rarity));
            if (hasSetNames) {
                unionPriceWhere.and(QueryDslFilterHelper.inIfNotEmpty(query.getSetNames(), p.setName));
            }
            productTypeMatch.or(p.tableName.eq(ProductTableEnum.UNION_PRICE)
                    .and(p.sourceId.in(
                            JPAExpressions.select(p.unionPriceId)
                                    .from(p.unionPrice)
                                    .where(unionPriceWhere))));

            // 레어리티 필터가 없을 때만 sealed 상품 세트명 매칭 (sealed에는 rarity 없음)
            if (hasSetNames && !hasRarities) {
                productTypeMatch.or(p.tableName.eq(ProductTableEnum.SEALED_PRODUCT)
                        .and(p.sourceId.in(
                                JPAExpressions.select(p.sealedProductId)
                                        .from(p.sealedProduct)
                                        .where(QueryDslFilterHelper.inIfNotEmpty(
                                                query.getSetNames(), p.sealedSetName)))));
            }

            where.and(productTypeMatch);
        }

        return where;
    }

    /**
     * 상품 검색·facet 집계 쿼리.
     * <p>
     * 단계 요약: (0) 검색·facet과 동일한 경로·WHERE 생성,
     * (1) (sourceId, tableName) 기준 그룹 수 = 페이지 total,
     * (2) 그룹별 대표 id.min()으로 페이지 슬라이스,
     * (3) 대표 id로 실제 엔티티 적재 후 그룹 순서 유지.
     */
    @Override
    public Page<ProductSearchMap> findByProductSearchingDto(ProductSearchingDto query, Pageable pageable) {
        // 1) 검색·facet과 동일한 경로·WHERE 생성
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);

        // 2) (sourceId, tableName) 기준 그룹 수 = 페이지 total
        StringExpression groupKey = Expressions.stringTemplate("CONCAT({0}, '::', {1})",
                p.sourceId, p.tableName);
        Long totalRow = queryFactory.select(groupKey.countDistinct())
                .from(p.map)
                .where(where)
                .fetchOne();
        long total = totalRow != null ? totalRow : 0L;

        // 3) 그룹별 대표 id.min()으로 페이지 슬라이스
        List<OrderSpecifier<?>> orderSpecifiers = toGroupOrder(
                p.id,
                p.sortPrice,
                p.productName,
                p.sourceId,
                p.tableName,
                p.unionPrice,
                p.unionPriceId,
                p.unionSetNumber,
                pageable);
        List<Long> pageRepresentativeIds = queryFactory.select(p.id.min())
                .from(p.map)
                .where(where)
                .groupBy(p.sourceId, p.tableName)
                .orderBy(orderSpecifiers.toArray(OrderSpecifier[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (pageRepresentativeIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        // 4) 대표 id로 실제 엔티티 적재 후 그룹 순서 유지
        List<ProductSearchMap> rows = queryFactory.selectFrom(p.map)
                .where(p.id.in(pageRepresentativeIds))
                .fetch();
        Map<Long, ProductSearchMap> byId = rows.stream()
                .collect(Collectors.toMap(ProductSearchMap::getId, m -> m));
        List<ProductSearchMap> content = pageRepresentativeIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<String> findDistinctGamesForSearch(ProductSearchingDto query) {
        // 1) 목록 검색과 동일 WHERE
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        // 2) 매칭 행에서 game만 distinct + 정렬
        return queryFactory.select(p.game).distinct()
                .from(p.map)
                .where(where.and(p.game.isNotNull()))
                .orderBy(p.game.asc())
                .fetch();
    }

    @Override
    public List<String> findDistinctProductTypesForSearch(ProductSearchingDto query) {
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        return queryFactory.select(p.productType).distinct()
                .from(p.map)
                .where(where.and(p.productType.isNotNull()))
                .orderBy(p.productType.asc())
                .fetch();
    }

    @Override
    public List<String> findDistinctSuppliesTypesForSearch(ProductSearchingDto query) {
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        return queryFactory.select(p.suppliesType).distinct()
                .from(p.map)
                .where(where.and(p.suppliesType.isNotNull()))
                .orderBy(p.suppliesType.asc())
                .fetch();
    }

    @Override
    public List<String> findDistinctManualCategoriesForSearch(ProductSearchingDto query) {
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        return queryFactory.select(p.manualCategory).distinct()
                .from(p.map)
                .where(where
                        .and(p.tableName.eq(ProductTableEnum.MANUAL_PRODUCT))
                        .and(p.manualCategory.isNotNull()))
                .orderBy(p.manualCategory.asc())
                .fetch();
    }

    @Override
    public List<String> findDistinctPrintTypesForSearch(ProductSearchingDto query) {
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        return queryFactory.select(p.printType).distinct()
                .from(p.map)
                .where(where.and(p.printType.isNotNull()))
                .orderBy(p.printType.asc())
                .fetch();
    }

    @Override
    public List<Long> findDistinctUnionPriceSourceIdsForSearch(ProductSearchingDto query) {
        // 1) 동일 WHERE로 후보 행 제한
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        // 2) 카드 가격행만 sourceId(UnionPrice PK) 수집 → 레어·세트명은 UnionPrice 쪽 distinct 재사용
        return queryFactory.select(p.sourceId).distinct()
                .from(p.map)
                .where(where
                        .and(p.tableName.eq(ProductTableEnum.UNION_PRICE))
                        .and(p.sourceId.isNotNull()))
                .fetch();
    }

    @Override
    public List<Long> findDistinctSealedProductSourceIdsForSearch(ProductSearchingDto query) {
        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = buildProductSearchWhere(query, p);
        return queryFactory.select(p.sourceId).distinct()
                .from(p.map)
                .where(where
                        .and(p.tableName.eq(ProductTableEnum.SEALED_PRODUCT))
                        .and(p.sourceId.isNotNull()))
                .fetch();
    }

    private List<OrderSpecifier<?>> toGroupOrder(
            NumberPath<Long> id,
            NumberPath<java.math.BigDecimal> sortPrice,
            StringPath productName,
            NumberPath<Long> sourceId,
            EnumPath<ProductTableEnum> tableName,
            PathBuilder<UnionPrice> unionPrice,
            NumberPath<Long> unionPriceId,
            NumberPath<Long> unionSetNumber,
            Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort == null || sort.isEmpty()) {
            return defaultGroupOrder(id, sourceId, tableName, unionPrice, unionPriceId, unionSetNumber);
        }
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        OrderSpecifier<Integer> setNumberNullOrder = unionSetNumberNullOrder(sourceId, tableName, unionPrice, unionPriceId);
        NumberExpression<Long> setNumberOrderExpression = unionSetNumberOrderExpression(
                sourceId,
                tableName,
                unionPrice,
                unionPriceId,
                unionSetNumber);
        for (Sort.Order o : sort) {
            String prop = o.getProperty();
            boolean asc = o.getDirection().isAscending();
            if (PROP_SET_NUMBER.equalsIgnoreCase(prop)) {
                orders.add(setNumberNullOrder);
                orders.add(asc ? setNumberOrderExpression.asc() : setNumberOrderExpression.desc());
            } else if (PROP_SORT_PRICE.equalsIgnoreCase(prop)) {
                if (asc) {
                    orders.add(sortPrice.min().asc());
                } else {
                    orders.add(sortPrice.max().desc());
                }
            } else if (PROP_PRODUCT_NAME.equalsIgnoreCase(prop)) {
                if (asc) {
                    orders.add(productName.min().asc());
                } else {
                    orders.add(productName.max().desc());
                }
            } else if (PROP_ID.equalsIgnoreCase(prop)) {
                if (asc) {
                    orders.add(id.min().asc());
                } else {
                    orders.add(id.max().desc());
                }
            }
        }
        if (orders.isEmpty()) {
            return defaultGroupOrder(id, sourceId, tableName, unionPrice, unionPriceId, unionSetNumber);
        }
        orders.add(id.min().asc());
        return orders;
    }

    private List<OrderSpecifier<?>> defaultGroupOrder(
            NumberPath<Long> id,
            NumberPath<Long> sourceId,
            EnumPath<ProductTableEnum> tableName,
            PathBuilder<UnionPrice> unionPrice,
            NumberPath<Long> unionPriceId,
            NumberPath<Long> unionSetNumber) {
        return List.of(
                unionSetNumberNullOrder(sourceId, tableName, unionPrice, unionPriceId),
                unionSetNumberOrderExpression(sourceId, tableName, unionPrice, unionPriceId, unionSetNumber).asc(),
                id.min().asc());
    }

    private OrderSpecifier<Integer> unionSetNumberNullOrder(
            NumberPath<Long> sourceId,
            EnumPath<ProductTableEnum> tableName,
            PathBuilder<UnionPrice> unionPrice,
            NumberPath<Long> unionPriceId) {
        return Expressions.cases()
                .when(tableName.eq(ProductTableEnum.UNION_PRICE)
                        .and(JPAExpressions.selectOne()
                                .from(unionPrice)
                                .where(unionPriceId.eq(sourceId)
                                        .and(unionPrice.getNumber(PROP_SET_NUMBER, Long.class).isNotNull()))
                                .exists()))
                .then(0)
                .otherwise(1)
                .asc();
    }

    @Override
    public List<ProductSearchMap> findByPrefixForSuggest(String prefix, int limit) {
        if (prefix == null || prefix.isBlank() || limit <= 0) {
            return List.of();
        }

        SearchPaths p = SearchPaths.create();
        BooleanBuilder where = new BooleanBuilder();
        where.and(p.isVisible.eq(true));
        where.and(p.tableName.ne(ProductTableEnum.CARD_PRODUCT));
        where.and(p.game.isNull()
                .or(p.game.notIn(GameEnum.temporarilyHiddenFromCustomerSearchGameNames())));
        where.and(p.productName.startsWithIgnoreCase(prefix)
                .or(p.productNameKo.startsWithIgnoreCase(prefix)));

        return queryFactory.selectFrom(p.map)
                .where(where)
                .orderBy(p.productName.asc())
                .limit(limit)
                .fetch();
    }

    private NumberExpression<Long> unionSetNumberOrderExpression(
            NumberPath<Long> sourceId,
            EnumPath<ProductTableEnum> tableName,
            PathBuilder<UnionPrice> unionPrice,
            NumberPath<Long> unionPriceId,
            NumberPath<Long> unionSetNumber) {
        return Expressions.numberTemplate(
                Long.class,
                "coalesce(({0}), {1})",
                JPAExpressions.select(unionSetNumber)
                        .from(unionPrice)
                        .where(tableName.eq(ProductTableEnum.UNION_PRICE)
                                .and(unionPriceId.eq(sourceId))),
                Long.MAX_VALUE);
    }
}
