package com.shop.product.service.supply;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.shop.product.entity.supplies.SupplyType;
import com.shop.product.repository.supply.SupplyTypeRepository;
import com.shop.search.dto.searching.SuppliesTypeFacetDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SuppliesTypeLabelResolver {

    private final SupplyTypeRepository supplyTypeRepository;

    public SuppliesTypeFacetDto resolveFacet(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }

        return supplyTypeRepository.findByNameKo(storedValue)
                .or(() -> supplyTypeRepository.findByNameEn(storedValue))
                .map(this::toFacetDto)
                .orElseGet(() -> SuppliesTypeFacetDto.builder()
                        .nameEn(storedValue)
                        .nameKo(storedValue)
                        .build());
    }

    public List<SuppliesTypeFacetDto> resolveFacets(List<String> storedValues) {
        if (storedValues == null || storedValues.isEmpty()) {
            return List.of();
        }

        Map<String, SupplyType> byNameKo = indexBy(
                supplyTypeRepository.findByNameKoIn(storedValues),
                SupplyType::getNameKo);
        List<String> unresolved = storedValues.stream()
                .filter(value -> !byNameKo.containsKey(value))
                .toList();
        Map<String, SupplyType> byNameEn = indexBy(
                supplyTypeRepository.findByNameEnIn(unresolved),
                SupplyType::getNameEn);

        return storedValues.stream()
                .map(value -> {
                    SupplyType supplyType = byNameKo.get(value);
                    if (supplyType == null) {
                        supplyType = byNameEn.get(value);
                    }
                    if (supplyType != null) {
                        return toFacetDto(supplyType);
                    }
                    return SuppliesTypeFacetDto.builder()
                            .nameEn(value)
                            .nameKo(value)
                            .build();
                })
                .toList();
    }

    private SuppliesTypeFacetDto toFacetDto(SupplyType supplyType) {
        return SuppliesTypeFacetDto.builder()
                .nameEn(supplyType.getNameEn())
                .nameKo(supplyType.getNameKo())
                .build();
    }

    private Map<String, SupplyType> indexBy(Collection<SupplyType> supplyTypes, Function<SupplyType, String> keyFn) {
        return supplyTypes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        keyFn,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }
}
