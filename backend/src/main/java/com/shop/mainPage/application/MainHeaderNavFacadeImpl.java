package com.shop.mainPage.application;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.metadata.dto.FabSetInfoDto;
import com.shop.card.metadata.repository.FabSetInfoRepository;
import com.shop.card.metadata.repository.MtgSetInfoRepository;
import com.shop.mainPage.dto.HeaderNavGameSetsDto;
import com.shop.mainPage.dto.HeaderNavSetDto;
import com.shop.product.enums.GameEnum;
import com.shop.product.metadata.repository.TcgPSetNameRepository;
import com.shop.product.metadata.repository.TcgPSyncGameRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MainHeaderNavFacadeImpl implements MainHeaderNavFacade {

        private static final List<GameEnum> HEADER_NAV_ORDER = List.of(
                        GameEnum.MTG, GameEnum.FAB, GameEnum.SWU, GameEnum.LORC, GameEnum.RIFT);

        private final MtgSetInfoRepository mtgSetInfoRepository;
        private final FabSetInfoRepository fabSetInfoRepository;
        private final TcgPSyncGameRepository tcgPSyncGameRepository;
        private final TcgPSetNameRepository tcgPSetNameRepository;

        @Override
        @Transactional(readOnly = true)
        public List<HeaderNavGameSetsDto> getLatestSetsByGame(int limit) {
                int size = Math.max(1, limit);

                Map<GameEnum, Long> gameToProductLineId = tcgPSyncGameRepository.findAll().stream()
                                .filter(game -> game != null && game.getProductLineId() != null)
                                .map(game -> GameEnum.fromProductLineId(game.getProductLineId())
                                                .map(g -> Map.entry(g, game.getProductLineId()))
                                                .orElse(null))
                                .filter(entry -> entry != null)
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                (first, second) -> first,
                                                LinkedHashMap::new));

                return HEADER_NAV_ORDER.stream()
                                .filter(game -> game == GameEnum.MTG || game == GameEnum.FAB
                                                || gameToProductLineId.containsKey(game))
                                .map(game -> new HeaderNavGameSetsDto(
                                                game.getGameAbbr(),
                                                resolveLatestSets(game, gameToProductLineId.get(game), size)))
                                .toList();
        }

        // 발매일이 미래인 세트 최대 3주 후까지만 표시. mtg는 세트 타입이 memoribilia, tokens인 세트도 표시하지 않음.
        private List<HeaderNavSetDto> resolveLatestSets(GameEnum game, Long productLineId, int limit) {
                if (game == GameEnum.MTG) {
                        return mtgSetInfoRepository
                                        .findAllByReleaseDateBeforeOrderByReleaseDateDesc(
                                                        LocalDate.now().plusWeeks(3).atStartOfDay())
                                        .stream()
                                        .limit(limit)
                                        .map(setInfo -> new HeaderNavSetDto(setInfo.getSetCode(), setInfo.getName(),
                                                        null))
                                        .toList();
                }
                if (game == GameEnum.FAB) {
                        return fabSetInfoRepository
                                        .mainHeaderNavSetsByCategoryOrderByPorderDesc()
                                        .stream()
                                        .limit(limit)
                                        .map(FabSetInfoDto::from)
                                        .map(setInfo -> new HeaderNavSetDto(setInfo.getSetCode(), setInfo.getName(),
                                                        null))
                                        .toList();
                }
                return tcgPSetNameRepository
                                .findSetInfoListByProductLineIdAndReleaseDateBeforeOrderByReleaseDateDesc(
                                                productLineId, LocalDate.now().plusWeeks(3).atStartOfDay())
                                .stream()
                                .limit(limit)
                                .map(setInfo -> new HeaderNavSetDto(setInfo.getSetCode(), setInfo.getSetName(),
                                                setInfo.getUrlName()))
                                .toList();
        }
}
