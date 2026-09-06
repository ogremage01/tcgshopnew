package com.shop.order.repository;

import org.springframework.data.domain.Page;

import com.shop.order.dto.OrderInfoListCriteria;
import com.shop.order.dto.OrderInfoSearchCriteria;
import com.shop.order.entity.OrderInfo;

public interface OrderInfoRepositoryCustom {

    Page<OrderInfo> findWithFilters(OrderInfoListCriteria criteria);

    Page<OrderInfo> findByKeyword(OrderInfoSearchCriteria criteria);
}
