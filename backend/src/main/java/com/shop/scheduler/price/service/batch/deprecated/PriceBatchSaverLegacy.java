package com.shop.scheduler.price.service.batch.deprecated;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;
import com.shop.card.repository.FabPriceRepository;
import com.shop.card.repository.MtgPriceRepository;
import com.shop.card.repository.TcgPPriceRepository;
import com.shop.scheduler.price.service.batch.InfoAddService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * [DEPRECATED] JPA merge 방식의 가격 배치 저장 클래스.
 *
 * 문제점:
 * 1. N+1 SELECT 문제 (개선 전)
 * - mergeMtgWithExisting: incoming N건 → findBySetAndCodeAndType SELECT N회
 * - mergeFabWithExisting: incoming N건 → findBySetAndCode SELECT N회
 *
 * 2. SELECT → Java merge → saveAll 흐름 (개선 후에도 남는 구조적 한계)
 * - DB에서 기존 행을 가져와 Java 메모리에서 필드를 덮어쓴 뒤 JPA에 맡기는 방식
 * - JPA 영속성 컨텍스트(1차 캐시)에 모든 행이 올라오므로 대용량 처리 시 GC 부담
 * - INSERT도 GenerationType.IDENTITY 전략 때문에 JDBC 배치 적용 불가
 * (IDENTITY 전략: INSERT 직후 DB가 생성한 id를 가져와야 하므로 배치 묶음 불가)
 *
 * 이 클래스는 학습·비교용으로만 보존한다. Spring Bean으로 등록하지 않는다(@Service 없음).
 * 실제 사용 클래스는 com.shop.scheduler.price.service.batch.PriceBatchSaver 참조.
 */
@Deprecated
@RequiredArgsConstructor
public class PriceBatchSaverLegacy {

    private final TcgPPriceRepository singlePriceRepository;
    private final InfoAddService infoAddService;
    private final MtgPriceRepository mtgPriceRepository;
    private final FabPriceRepository fabPriceRepository;

    @Deprecated
    @Transactional
    public void saveTcgBatch(List<TcgPPrice> incoming) {
        if (incoming.isEmpty()) {
            return;
        }
        singlePriceRepository.saveAll(mergeWithExisting(incoming));
    }

    @Deprecated
    @Transactional
    public void saveMtgBatch(List<MtgPrice> incoming) {
        if (incoming.isEmpty()) {
            return;
        }
        mtgPriceRepository.saveAll(mergeMtgWithExisting(incoming));
    }

    @Deprecated
    @Transactional
    public void saveFabBatch(List<FabPrice> incoming) {
        if (incoming.isEmpty()) {
            return;
        }
        fabPriceRepository.saveAll(mergeFabWithExisting(incoming));
    }

    /**
     * [개선 후] checkCode 기준으로 기존 행을 한 번에 조회(IN 쿼리)해 필드만 갱신한다.
     * 기존: incoming N건 → SELECT N회 / 개선: SELECT 1회
     *
     * [개선 전 문제] 아래처럼 루프 안에서 SELECT를 호출하면 N+1 문제가 발생한다.
     * for (MtgPrice row : incoming) {
     * Optional<MtgPrice> existing =
     * mtgPriceRepository.findBySetAndCodeAndType(...); // N번!
     * }
     */
    private List<MtgPrice> mergeMtgWithExisting(List<MtgPrice> incoming) {
        List<String> checkCodes = incoming.stream()
                .map(MtgPrice::getCheckCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, MtgPrice> existingByCode = checkCodes.isEmpty()
                ? Map.of()
                : mtgPriceRepository.findByCheckCodeIn(checkCodes).stream()
                        .collect(Collectors.toMap(MtgPrice::getCheckCode, Function.identity(), (a, b) -> a));

        List<MtgPrice> merged = new ArrayList<>(incoming.size());
        for (MtgPrice row : incoming) {
            MtgPrice existing = row.getCheckCode() != null ? existingByCode.get(row.getCheckCode()) : null;
            if (existing != null) {
                existing.setPrice(row.getPrice());
                existing.setCheckCode(row.getCheckCode());
                existing.setSetName(row.getSetName());
                existing.setName(row.getName());
                existing.setNameK(row.getNameK());
                merged.add(existing);
            } else {
                merged.add(row);
            }
        }
        return merged;
    }

    /**
     * [개선 후] (set, code) 복합키 기준으로 기존 행을 한 번에 조회(set IN 쿼리)해 필드만 갱신한다.
     * 기존: incoming N건 → SELECT N회 / 개선: distinct set 수만큼 SELECT (보통 1~수회)
     *
     * [개선 전 문제] 아래처럼 루프 안에서 SELECT를 호출하면 N+1 문제가 발생한다.
     * for (FabPrice row : incoming) {
     * Optional<FabPrice> existing = fabPriceRepository.findBySetAndCode(...); //
     * N번!
     * }
     */
    private List<FabPrice> mergeFabWithExisting(List<FabPrice> incoming) {
        List<String> sets = incoming.stream()
                .map(FabPrice::getSet)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, FabPrice> existingByKey = sets.isEmpty()
                ? Map.of()
                : fabPriceRepository.findBySetIn(sets).stream()
                        .collect(Collectors.toMap(
                                f -> f.getSet() + "\u001f" + f.getCode(),
                                Function.identity(),
                                (a, b) -> a));

        List<FabPrice> merged = new ArrayList<>(incoming.size());
        for (FabPrice row : incoming) {
            String key = row.getSet() + "\u001f" + row.getCode();
            FabPrice existing = existingByKey.get(key);
            if (existing != null) {
                existing.setPrice(row.getPrice());
                existing.setRarity(row.getRarity());
                existing.setSetName(row.getSetName());
                existing.setCardName(row.getCardName());
                existing.setCollectorNum(row.getCollectorNum());
                existing.setFoil(row.getFoil());
                existing.setCheckCode(row.getCheckCode());
                merged.add(existing);
            } else {
                merged.add(row);
            }
        }
        return merged;
    }

    /**
     * (productId, condition, printing) 단위로 기존 행을 찾아 필드만 갱신한다.
     *
     * JPA saveAll은 id가 null이면 항상 INSERT를 시도한다.
     * API에서 온 행은 id가 없으므로, 직접 기존 행을 조회해 갱신해야 한다.
     */
    private List<TcgPPrice> mergeWithExisting(List<TcgPPrice> incoming) {
        List<Long> productIds = incoming.stream()
                .map(TcgPPrice::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, TcgPPrice> existingByKey = singlePriceRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(PriceBatchSaverLegacy::rowKey, Function.identity(), (a, b) -> a));
        List<TcgPPrice> merged = new ArrayList<>(incoming.size());
        for (TcgPPrice row : incoming) {
            if (row == null || row.getProductId() == null) {
                continue;
            }
            TcgPPrice existing = existingByKey.get(rowKey(row));
            if (existing != null) {
                existing.setMarketPrice(normalizeMarketPrice(row.getMarketPrice()));
                merged.add(existing);
            } else {
                enrichDerivedFields(row);
                row.setMarketPrice(normalizeMarketPrice(row.getMarketPrice()));
                merged.add(row);
            }
        }
        return merged;
    }

    private void enrichDerivedFields(TcgPPrice price) {
        price.setCodeNumber(
                infoAddService.generateCodeNumber(price));
        price.setPrintType(infoAddService.getPrintType(price.getPrinting()));
        switch (price.getGame()) {
            case "Magic":
                price.setCheckCode(infoAddService.buildCheckCodeMtgForTcgP(price));
                break;
            case "Flesh & Blood TCG":
                price.setCheckCode(infoAddService.buildCheckCodeFabForTcgP(price));
                break;
            default:
                break;
        }
    }

    private static String rowKey(TcgPPrice s) {
        return Objects.toString(s.getProductId(), "")
                + "\u001f"
                + Objects.toString(s.getCondition(), "")
                + "\u001f"
                + Objects.toString(s.getPrinting(), "");
    }

    private static BigDecimal normalizeMarketPrice(BigDecimal raw) {
        if (raw == null) {
            return null;
        }
        return new BigDecimal(new BigDecimal(raw.toString()).toPlainString());
    }
}
