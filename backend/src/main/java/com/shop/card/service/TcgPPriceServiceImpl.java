package com.shop.card.service;

import org.springframework.stereotype.Service;

import com.shop.card.entity.TcgPPrice;
import com.shop.card.repository.TcgPPriceRepository;
import com.shop.product.dto.card.full.TcgPPriceDto;
import com.shop.product.dto.card.slim.TcgPPriceCardSlimDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Slf4j
public class TcgPPriceServiceImpl implements TcgPPriceService {
    private final TcgPPriceRepository tcgPPriceRepository;

    @Override
    public List<TcgPPriceCardSlimDto> findByProductName(String productName) {
        return tcgPPriceRepository.findByProductName(productName);
    }

    @Override
    public Optional<TcgPPriceCardSlimDto> findById(Long id) {
        TcgPPrice price = tcgPPriceRepository.findById(id).orElse(null);
        if (price == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(TcgPPriceCardSlimDto.builder()
                .id(price.getId())
                .productId(price.getProductId())
                .game(price.getGame())
                .productName(price.getProductName())
                .type(price.getType())
                .rarity(price.getRarity())
                .marketPrice(price.getMarketPrice())
                .isDoubleSided(price.getIsDoubleSided())
                .printType(price.getPrintType())
                .printing(price.getPrinting())
                .build());
    }

    @Override
    public Page<TcgPPriceCardSlimDto> findByProductNameOrCodeNumber(String keyword, Pageable pageable) {
        return tcgPPriceRepository.findByProductNameOrCodeNumber(keyword, pageable);
    }

    @Override
    public List<TcgPPriceDto> findByGameAndSetNameForExcelExport(String game, String set) {
        return tcgPPriceRepository.findByGameAndSetNameForExcelExport(game, set).stream()
                .map(tcgPrice -> TcgPPriceDto.builder()
                        .id(tcgPrice.getId())
                        .productId(tcgPrice.getProductId())
                        .game(tcgPrice.getGame())
                        .productName(tcgPrice.getProductName())
                        .type(tcgPrice.getType())
                        .rarity(tcgPrice.getRarity())
                        .marketPrice(tcgPrice.getMarketPrice())
                        .number(tcgPrice.getNumber())
                        .printing(tcgPrice.getPrinting())
                        .set(tcgPrice.getSet())
                        .setAbbrv(tcgPrice.getSetAbbrv())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TcgPPriceCardSlimDto> findByProductNameOrCodeNumberList(String keyword) {
        return tcgPPriceRepository.findByProductNameOrCodeNumberList(keyword);
    }
}
