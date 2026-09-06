package com.shop.product.repository.card;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.product.entity.card.CardProductLanguage;

public interface CardProductLanguageRepository extends JpaRepository<CardProductLanguage, Long> {

    List<CardProductLanguage> findByIsActiveTrueOrderByCodeAsc();
}
