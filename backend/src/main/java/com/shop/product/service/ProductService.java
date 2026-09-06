package com.shop.product.service;

import org.springframework.stereotype.Service;

import com.shop.product.dto.card.management.CardProductRegisterCommand;

import java.util.Collections;
import java.util.List;

@Service
public class ProductService {

    public List<Object> findAll() {
        // TODO: Product 기능은 추후 구현 예정
        return Collections.emptyList();
    }

    public Object findById(Long id) {
        // TODO: Product 기능은 추후 구현 예정
        return null;
    }

    /**
     * 이름 기준 검색. Redis 캐시 적용 시 이 메서드에 @Cacheable(key = "#name", unless =
     * "#result.isEmpty()") 등으로 캐싱 가능.
     */
    public List<Object> searchByName(String name) {
        // TODO: Product 기능은 추후 구현 예정
        return Collections.emptyList();
    }

    public void registerProduct(CardProductRegisterCommand cardProductRegisterCommand) {
        // TODO: Product 기능은 추후 구현 예정
    }
}
