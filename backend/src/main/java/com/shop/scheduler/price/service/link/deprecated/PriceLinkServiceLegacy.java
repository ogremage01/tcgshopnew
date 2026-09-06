package com.shop.scheduler.price.service.link.deprecated;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;
import com.shop.card.repository.FabPriceRepository;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.card.repository.TcgPPriceRepository;
import com.shop.scheduler.price.service.batch.InfoAddService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [DEPRECATED] JPA + Java 메모리 매칭 방식의 tcgPPriceId 연결 클래스.
 *
 * 문제점:
 * 두 테이블을 각각 전체 SELECT한 뒤 Java 메모리에서 HashMap으로 매칭한다.
 * tcg_p_prices의 game='Magic' 행이 수십만 건이라면
 * 전부 메모리에 올라오므로 GC 부담과 처리 시간이 크다.
 *
 * 흐름:
 * SELECT mtg_prices WHERE tcg_p_price_id IS NULL ← 전체 로드
 * SELECT tcg_p_prices WHERE game = 'Magic' ← 전체 로드 (수십만 건)
 * Java HashMap에서 check_code 기준 매칭
 * saveAll(matched) ← 매칭된 행만 UPDATE
 *
 * 이 클래스는 학습·비교용으로만 보존한다. Spring Bean으로 등록하지 않는다(@Service 없음).
 * 실제 사용 클래스는 com.shop.scheduler.price.service.link.PriceLinkService 참조.
 */
@Deprecated
@RequiredArgsConstructor
public class PriceLinkServiceLegacy {

    private final MtgPriceRepository mtgPriceRepository;
    private final TcgPPriceRepository tcgPriceRepository;
    private final FabPriceRepository fabPriceRepository;
    private final InfoAddService infoAddService;

    /**
     * mtg_prices 중 tcgPPriceId가 없는 행에 매칭되는 tcg_p_prices.id와 conNumName을 설정한다.
     *
     * [한계]
     * tcg_p_prices 전체(game='Magic')를 Java 메모리에 올린 뒤 HashMap으로 매칭한다.
     * DB가 훨씬 효율적으로 처리할 수 있는 JOIN 연산을 Java에서 수행하는 구조.
     * 데이터가 많아질수록 메모리·시간 비용이 선형 증가한다.
     */
    @Deprecated
    @Transactional
    public void syncMtgPriceWithTcgPrice() {
        List<MtgPrice> mtgPrices = mtgPriceRepository.findByTcgPPriceIdIsNull();
        List<TcgPPrice> tcgPrices = tcgPriceRepository.findByGame("Magic");

        // check_code → MtgPrice 맵 구성
        Map<String, MtgPrice> mtgPriceByCode = new HashMap<>();
        for (MtgPrice mtgPrice : mtgPrices) {
            String code = infoAddService.buildCheckCodeMtgForMtgPrice(mtgPrice);
            if (code != null) {
                mtgPriceByCode.put(code, mtgPrice);
            }
        }

        // tcg_p_prices를 순회하며 check_code로 매칭
        List<MtgPrice> matched = new ArrayList<>();
        for (TcgPPrice tcgPrice : tcgPrices) {
            String tcgCode = infoAddService.buildCheckCodeMtgForTcgP(tcgPrice);
            // NON_ENGLISH_FAILURE: condition에 "-"가 포함된 행(비영어권 카드)은 건너뜀
            if (tcgCode == null || tcgCode.equals(InfoAddService.NON_ENGLISH_FAILURE)) {
                continue;
            }
            MtgPrice mtgPrice = mtgPriceByCode.get(tcgCode);
            if (mtgPrice != null) {
                mtgPrice.setTcgPPriceId(tcgPrice.getId());
                mtgPrice.setConNumName(
                        tcgPrice.getCondition() + "-" + tcgPrice.getNumber() + "-" + tcgPrice.getProductName());
                matched.add(mtgPrice);
            }
        }

        mtgPriceRepository.saveAll(matched);
    }

    /**
     * fab_prices 중 tcgPPriceId가 없는 행에 매칭되는 tcg_p_prices.id를 설정한다.
     *
     * [한계] syncMtgPriceWithTcgPrice와 동일한 구조적 한계를 가진다.
     */
    @Deprecated
    @Transactional
    public void syncFabPriceWithTcgPrice() {
        List<FabPrice> fabPrices = fabPriceRepository.findByTcgPPriceIdIsNull();
        List<TcgPPrice> tcgPrices = tcgPriceRepository.findByGame("Flesh & Blood TCG");

        Map<String, FabPrice> fabPriceByCode = new HashMap<>();
        for (FabPrice fabPrice : fabPrices) {
            fabPriceByCode.put(infoAddService.buildCheckCodeFabForFabPrice(fabPrice), fabPrice);
        }

        List<FabPrice> matched = new ArrayList<>();
        for (TcgPPrice tcgPrice : tcgPrices) {
            String tcgCode = infoAddService.buildCheckCodeFabForTcgP(tcgPrice);
            FabPrice fabPrice = fabPriceByCode.get(tcgCode);
            if (fabPrice != null) {
                fabPrice.setTcgPPriceId(tcgPrice.getId());
                fabPrice.setConNumName(
                        tcgPrice.getCondition() + "-" + tcgPrice.getNumber() + "-" + tcgPrice.getProductName());
                matched.add(fabPrice);
            }
        }

        fabPriceRepository.saveAll(matched);
    }
}
