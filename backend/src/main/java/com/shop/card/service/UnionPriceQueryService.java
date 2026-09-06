package com.shop.card.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.shop.card.dto.slim.UnionPriceAdminSearchResponseDto;
import com.shop.card.entity.UnionPrice;

/** {@code union_prices} 조회 전용 (엑셀·관리 화면 등). */
public interface UnionPriceQueryService {

    /** game + 세트 식별자(setCode 우선 권장, 없으면 setName과 동일하게 OR 조회). */
    List<UnionPrice> findByGameAndSetNameAndPrintType(String game, String setCodeOrName, String printType);

    UnionPrice findById(Long id);

    UnionPriceAdminSearchResponseDto searchByKeywordForAdmin(String keyword, String game, String setCode,
            Pageable pageable);

}
