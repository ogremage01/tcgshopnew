package com.shop.order.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.shop.common.page.dto.PageParam;
import com.shop.order.dto.OrderInfoListCriteria;
import com.shop.order.dto.OrderInfoSearchCriteria;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.QOrderInfo;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderInfoRepositoryImpl implements OrderInfoRepositoryCustom {

    private static final String PROP_ID = "id";
    private static final String PROP_PAYMENT_DATE = "paymentDate";
    private static final String PROP_PAYMENT_APPROVED_AT = "paymentApprovedAt";
    private static final String PROP_ORDER_STATUS = "orderStatus";

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderInfo> findWithFilters(OrderInfoListCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(criteria.getPageParam(), "pageParam");
        QOrderInfo o = QOrderInfo.orderInfo;
        return fetchPage(o, buildListPredicate(o, criteria), criteria.getPageParam());
    }

    @Override
    public Page<OrderInfo> findByKeyword(OrderInfoSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(criteria.getPageParam(), "pageParam");
        Objects.requireNonNull(criteria.getKeyword(), "keyword");
        QOrderInfo o = QOrderInfo.orderInfo;
        return fetchPage(o, buildSearchPredicate(o, criteria.getKeyword()), criteria.getPageParam());
    }

    private Page<OrderInfo> fetchPage(QOrderInfo o, BooleanBuilder predicate, PageParam pp) {
        Integer pageValue = pp.getPage();
        Integer sizeValue = pp.getSize();
        int page = pageValue != null ? pageValue.intValue() : 0;
        int size = sizeValue != null ? sizeValue.intValue() : 20;
        Sort sort = pp.toSort();
        Pageable pageable = PageRequest.of(page, size, sort);

        /*
         * 조건이 하나도 없을 때 빈 BooleanBuilder를 where에 넣으면 QueryDSL/버전에 따라
         * 결과가 0건으로 나가는 경우가 있어, predicate가 비었으면 where 자체를 생략한다.
         */
        var countQuery = queryFactory.select(o.id.count()).from(o);
        var listQuery = queryFactory.selectFrom(o);
        if (predicate.hasValue()) {
            countQuery.where(predicate);
            listQuery.where(predicate);
        }

        Long totalRow = countQuery.fetchOne();
        long total = totalRow != null ? totalRow : 0L;

        List<OrderInfo> content = listQuery
                .orderBy(orderSpecifiers(o, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private static BooleanBuilder buildListPredicate(QOrderInfo o, OrderInfoListCriteria c) {
        BooleanBuilder builder = new BooleanBuilder();

        if (c.getStartDate() != null) {
            builder.and(orderAtGoe(o, c.getStartDate()));
        }
        if (c.getEndDate() != null) {
            builder.and(orderAtLoe(o, c.getEndDate()));
        }
        if (c.getOrderStatusList() != null && !c.getOrderStatusList().isEmpty()) {
            List<String> normalizedStatuses = c.getOrderStatusList().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());
            if (!normalizedStatuses.isEmpty()) {
                builder.and(Expressions.stringTemplate("upper(trim({0}))", o.orderStatus).in(normalizedStatuses));
            }
        }
        return builder;
    }

    private static BooleanBuilder buildSearchPredicate(QOrderInfo o, String keyword) {
        BooleanBuilder builder = new BooleanBuilder();
        BooleanExpression nameMatch = o.recipientName.containsIgnoreCase(keyword);
        Long orderId = parseNumericOrderId(keyword);
        if (orderId != null) {
            builder.and(o.id.eq(orderId).or(nameMatch));
        } else {
            builder.and(nameMatch);
        }
        return builder;
    }

    /** 숫자만으로 이루어진 검색어면 주문 id로 파싱. 아니면 null. */
    private static Long parseNumericOrderId(String keyword) {
        if (keyword.isEmpty()) {
            return null;
        }
        for (int i = 0; i < keyword.length(); i++) {
            if (!Character.isDigit(keyword.charAt(i))) {
                return null;
            }
        }
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 목록 기준 시각(결제일 우선, 없으면 승인시각) >= start.
     * CaseBuilder 대신 명시적 OR로 작성해 DB별 CASE 해석 차이를 피한다.
     */
    private static BooleanExpression orderAtGoe(QOrderInfo o, java.time.LocalDateTime start) {
        return o.paymentDate.isNotNull().and(o.paymentDate.goe(start))
                .or(o.paymentDate.isNull().and(o.paymentApprovedAt.goe(start)));
    }

    /**
     * 목록 기준 시각(결제일 우선, 없으면 승인시각) <= end.
     */
    private static BooleanExpression orderAtLoe(QOrderInfo o, java.time.LocalDateTime end) {
        return o.paymentDate.isNotNull().and(o.paymentDate.loe(end))
                .or(o.paymentDate.isNull().and(o.paymentApprovedAt.loe(end)));
    }

    private static OrderSpecifier<?>[] orderSpecifiers(QOrderInfo o, Sort sort) {
        if (sort == null || sort.isEmpty()) {
            return new OrderSpecifier<?>[] { o.id.desc() };
        }
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order ord : sort) {
            String prop = ord.getProperty();
            boolean asc = ord.isAscending();
            switch (prop) {
                case PROP_ID -> orders.add(asc ? o.id.asc() : o.id.desc());
                case PROP_PAYMENT_DATE -> orders.add(asc ? o.paymentDate.asc() : o.paymentDate.desc());
                case PROP_PAYMENT_APPROVED_AT ->
                    orders.add(asc ? o.paymentApprovedAt.asc() : o.paymentApprovedAt.desc());
                case PROP_ORDER_STATUS -> orders.add(asc ? o.orderStatus.asc() : o.orderStatus.desc());
                default -> {
                    /* 알 수 없는 정렬 필드는 무시 */
                }
            }
        }
        if (orders.isEmpty()) {
            return new OrderSpecifier<?>[] { o.id.desc() };
        }
        return orders.toArray(OrderSpecifier[]::new);
    }
}
