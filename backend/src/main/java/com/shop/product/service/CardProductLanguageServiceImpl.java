package com.shop.product.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.shop.product.dto.card.CardProductLanguageDto;
import com.shop.product.repository.card.CardProductLanguageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CardProductLanguageServiceImpl implements CardProductLanguageService {

    private final CardProductLanguageRepository cardProductLanguageRepository;

    @Override
    public List<CardProductLanguageDto> findActiveLanguages() {
        return cardProductLanguageRepository.findByIsActiveTrueOrderByCodeAsc().stream()
                .map(CardProductLanguageDto::from)
                .sorted(Comparator.comparing(CardProductLanguageDto::getCode, CardProductLanguageServiceImpl::compareCode))
                .toList();
    }

    private static int compareCode(String a, String b) {
        return Integer.compare(priority(a), priority(b)) != 0
                ? Integer.compare(priority(a), priority(b))
                : safe(a).compareTo(safe(b));
    }

    private static int priority(String code) {
        String normalized = safe(code);
        if ("en".equals(normalized)) {
            return 0;
        }
        if ("ko".equals(normalized)) {
            return 1;
        }
        return 2;
    }

    private static String safe(String code) {
        return code == null ? "" : code.toLowerCase();
    }
}
