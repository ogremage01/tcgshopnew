package com.shop.scheduler.price.source.openbinder.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.scheduler.price.dto.PriceRawDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObPriceParser {

    public List<MtgPrice> parseMtgPrices(List<PriceRawDto> mtgPriceRawDtos) {
        log.info("Parsing MtgPrices: {}", mtgPriceRawDtos);
        if (mtgPriceRawDtos == null || mtgPriceRawDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<MtgPrice> result = new ArrayList<>();
        for (PriceRawDto raw : mtgPriceRawDtos) {
            Map<String, Object> priceMap = raw.getPrice();
            if (priceMap == null || priceMap.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Object> entry : priceMap.entrySet()) {
                String typeKey = entry.getKey();
                if (typeKey == null || typeKey.isBlank() || entry.getValue() == null) {
                    continue;
                }
                BigDecimal priceVal;
                try {
                    priceVal = new BigDecimal(entry.getValue().toString().trim());
                } catch (Exception e) {
                    continue;
                }
                result.add(MtgPrice.builder()
                        .set(raw.getSet())
                        .code(raw.getCode())
                        .type(typeKey)
                        .price(priceVal)
                        .setName(raw.getSetName())
                        .name(raw.getName())
                        .nameK(raw.getNameK())
                        .rarity(raw.getRarity())
                        .build());
            }
        }
        return result;
    }

    public List<FabPrice> parseFabPrices(List<PriceRawDto> fabPriceRawDtos) {
        log.info("Parsing FabPrices: {}", fabPriceRawDtos);
        if (fabPriceRawDtos == null || fabPriceRawDtos.isEmpty()) {
            return Collections.emptyList();
        }
        List<FabPrice> result = new ArrayList<>();
        for (PriceRawDto raw : fabPriceRawDtos) {
            if (raw == null) {
                continue;
            }
            Map<String, Object> priceMap = raw.getPrice();
            if (priceMap == null || priceMap.isEmpty()) {
                continue;
            }
            Object priceValue = priceMap.values().stream()
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (priceValue == null) {
                continue;
            }
            String code = raw.getCode();
            if (code == null || code.isBlank()) {
                continue;
            }
            BigDecimal priceVal;
            try {
                priceVal = new BigDecimal(priceValue.toString().trim());
            } catch (Exception e) {
                continue;
            }
            result.add(FabPrice.builder()
                    .set(raw.getSet())
                    .code(code)
                    .price(priceVal)
                    .rarity(raw.getRarity())
                    .setName(raw.getSetName())
                    .cardName(raw.getName())
                    .collectorNum(code.replaceAll("-.*", "").trim())
                    .foil(normalizeFabFoilFinish(raw.getFinishes()))
                    .build());
        }
        return result;
    }

    private static String normalizeFabFoilFinish(String finish) {
        if (finish == null || finish.isBlank()) {
            return finish;
        }
        String upper = finish.toUpperCase();
        if (upper.contains("NON-FOIL") || upper.contains("NON FOIL")) {
            return "Normal";
        }
        return finish;
    }
}
