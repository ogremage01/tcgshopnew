package com.shop.search.helper;

import java.util.List;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.StringPath;

public final class QueryDslFilterHelper {

    private QueryDslFilterHelper(){

    }
    // 리스트에 값이 있을 경우 조건 추가
    public static BooleanExpression inIfNotEmpty(List<String> values, StringPath column){
        if (values == null || values.isEmpty()){
            return null;
        }
        return column.in(values);
    }

    // 불리언 값이 null이 아닐 경우 조건 추가
    public static BooleanExpression eqIfNotNull(Boolean value, BooleanPath column){
        if (value == null){
            return null;
        }
        return column.eq(value);
    }

    // 전체/포일/일반일 때:null은 필터 제외
    public static BooleanExpression printTypeIfFoilKnown(Boolean isFoil, StringPath column){
        if (isFoil == null){
            return null;
        }
        return column.containsIgnoreCase(isFoil ? "Foil" : "Normal");
    }
}
