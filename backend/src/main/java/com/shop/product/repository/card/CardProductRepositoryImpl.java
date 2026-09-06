package com.shop.product.repository.card;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.card.entity.UnionPrice;
import com.shop.product.dto.card.management.SearchBySetCriteriaDto;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.enums.GameEnum;
import com.shop.search.helper.QueryDslFilterHelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class CardProductRepositoryImpl implements CardProductRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<CardProduct> findBySetForAdmin(SearchBySetCriteriaDto criteria, Pageable pageable) {
        PathBuilder<CardProduct> cardProduct = new PathBuilder<>(CardProduct.class, "cardProduct");
        PathBuilder<UnionPrice> unionPriceJoinTarget = cardProduct.get("unionPrice", UnionPrice.class);
        PathBuilder<UnionPrice> unionPrice = new PathBuilder<>(UnionPrice.class, "unionPrice");
        StringPath unionPriceSetCode = unionPrice.getString("setCode");
        StringPath unionPriceGame = unionPrice.getString("game");
        NumberPath<Long> unionPriceSetNumber = unionPrice.getNumber("setNumber", Long.class);
        List<String> gameNames = GameEnum.expandSearchNames(
                criteria.getGame() == null || criteria.getGame().isBlank()
                        ? List.of()
                        : List.of(criteria.getGame()));

        Long totalRow = queryFactory
                .select(cardProduct.count())
                .from(cardProduct)
                .innerJoin(unionPriceJoinTarget, unionPrice)
                .where(
                        unionPriceSetCode.eq(criteria.getSet()),
                        QueryDslFilterHelper.inIfNotEmpty(gameNames, unionPriceGame),
                        storageEq(cardProduct, criteria.getStorageId()),
                        printTypeFilterEq(cardProduct, criteria.getPrintTypeFilter()),
                        isVisibleEq(cardProduct, criteria.getIsVisible()))
                .fetchOne();
        long total = totalRow != null ? totalRow : 0L;

        var content = queryFactory
                .selectFrom(cardProduct)
                .innerJoin(unionPriceJoinTarget, unionPrice)
                .fetchJoin()
                .where(
                        unionPriceSetCode.eq(criteria.getSet()),
                        QueryDslFilterHelper.inIfNotEmpty(gameNames, unionPriceGame),
                        storageEq(cardProduct, criteria.getStorageId()),
                        printTypeFilterEq(cardProduct, criteria.getPrintTypeFilter()),
                        isVisibleEq(cardProduct, criteria.getIsVisible()))
                .orderBy(unionPriceSetNumber.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression storageEq(PathBuilder<CardProduct> cardProduct, Long storageId) {
        if (storageId == null || storageId == 0L) {
            return null;
        }
        return cardProduct.getNumber("storageId", Long.class).eq(storageId);
    }

    private BooleanExpression isVisibleEq(PathBuilder<CardProduct> cardProduct, Boolean isVisible) {
        if (isVisible == null) {
            return null;
        }
        return cardProduct.getBoolean("isVisible").eq(isVisible);
    }

    private BooleanExpression printTypeFilterEq(PathBuilder<CardProduct> cardProduct, String printTypeFilter) {
        if (printTypeFilter == null || "all".equalsIgnoreCase(printTypeFilter)) {
            return null;
        }
        StringPath printType = cardProduct.getString("printType");
        if ("foil".equalsIgnoreCase(printTypeFilter)) {
            return QueryDslFilterHelper.printTypeIfFoilKnown(true, printType);
        }
        if ("normal".equalsIgnoreCase(printTypeFilter)) {
            return QueryDslFilterHelper.printTypeIfFoilKnown(false, printType);
        }
        return null;
    }
}
