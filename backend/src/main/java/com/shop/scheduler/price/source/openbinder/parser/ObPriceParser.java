package com.shop.scheduler.price.source.openbinder.parser;

import java.util.Collections;
import java.util.List;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.scheduler.price.dto.PriceRawDto;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 공개본 스텁. 외부 가격 JSON 파싱 규칙은 포함하지 않습니다.
 */
@Slf4j
public class ObPriceParser {

    public List<MtgPrice> parseMtgPrices(List<PriceRawDto> mtgPriceRawDtos) {
        log.warn("Portfolio snapshot: MTG price parse is omitted.");
        return Collections.emptyList();
    }

    public List<FabPrice> parseFabPrices(List<PriceRawDto> fabPriceRawDtos) {
        log.warn("Portfolio snapshot: FAB price parse is omitted.");
        return Collections.emptyList();
    }
}
