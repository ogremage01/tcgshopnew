package com.shop.scheduler.price.service.batch;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.card.entity.FabPrice;
import com.shop.card.entity.MtgPrice;
import com.shop.card.entity.TcgPPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 가격 데이터를 UPSERT(INSERT ... ON DUPLICATE KEY UPDATE)로 저장한다.
 *
 * ── 기존 방식(JPA merge)과의 비교 ────────────────────────────────────────────
 *
 * 기존: SELECT(기존 행 조회) → Java에서 필드 덮어쓰기 → saveAll(JPA dirty check)
 * 개선: DB에 직접 UPSERT 1회 → SELECT 완전 제거
 *
 * JPA 방식은 영속성 컨텍스트(1차 캐시)에 모든 행을 올린 뒤
 * 트랜잭션 종료 시점에 dirty check로 변경을 감지한다.
 * 대용량 처리 시 메모리 사용량이 크고, GenerationType.IDENTITY 때문에
 * JDBC batch INSERT도 불가능하다.
 *
 * ── UPSERT 동작 원리 ─────────────────────────────────────────────────────────
 *
 * INSERT INTO table (col1, col2, ...) VALUES (?, ?, ...)
 * ON DUPLICATE KEY UPDATE col1 = VALUES(col1), ...
 *
 * MariaDB(MySQL)는 INSERT 시 UNIQUE KEY 충돌을 감지한다.
 * - 충돌 없음 → 새 행 INSERT
 * - 충돌 발생 → ON DUPLICATE KEY UPDATE 절의 컬럼만 갱신
 *
 * 이 방식은 Java에서 SELECT로 기존 행을 가져오지 않아도 되므로
 * DB 왕복이 UPSERT 1회로 줄어든다.
 *
 * ── 전제 조건: UNIQUE 제약 ───────────────────────────────────────────────────
 *
 * UPSERT의 충돌 감지는 DB의 UNIQUE KEY를 기준으로 동작한다.
 * 따라서 각 테이블에 아래 UNIQUE 제약이 반드시 존재해야 한다.
 *
 * tcg_p_prices : UNIQUE(product_id, card_condition, printing, product_name)
 * mtg_prices : UNIQUE(set_code, code, type)
 * fab_prices : UNIQUE(set_code, code)
 *
 * 엔티티의 @Table(uniqueConstraints = ...) 에 선언되어 있으며,
 * ddl-auto: update 시 Hibernate가 자동으로 인덱스를 생성한다.
 * 단, 기존 데이터에 중복 행이 있으면 인덱스 생성이 실패하므로
 * 초기 적재 전 데이터 정합성을 먼저 확인해야 한다.
 *
 * ── JdbcTemplate.batchUpdate 동작 ────────────────────────────────────────────
 *
 * jdbcTemplate.batchUpdate(sql, List<Object[]>)
 * - Object[] 하나가 PreparedStatement의 파라미터 묶음 (한 행)
 * - 내부적으로 JDBC의 addBatch() / executeBatch()를 호출한다.
 * - MariaDB JDBC URL에 rewriteBatchedStatements=true 가 있으면
 * 드라이버가 여러 INSERT를 하나의 네트워크 패킷으로 합쳐 전송한다.
 * → application.yml datasource.url 참조
 *
 * ── JPA 영속성 컨텍스트 우회 주의사항 ────────────────────────────────────────
 *
 * JdbcTemplate은 JPA 영속성 컨텍스트를 거치지 않는다.
 * 같은 트랜잭션 내에서 JPA로 조회한 엔티티와 JdbcTemplate으로 저장한 데이터가
 * 일치하지 않을 수 있다(1차 캐시 불일치).
 * 이 클래스는 스케줄러에서만 호출되며 조회 후 곧바로 저장만 하므로 문제없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceBatchSaver {

    /** check_code_refined 중복 시 설정하는 플레이스홀더. JOIN 매칭에서 제외된다. */
    static final String DUPLICATE_CODE = "DUPLICATE_CODE";

    /**
     * Spring이 자동으로 주입하는 JDBC 직접 접근 유틸.
     * JPA(EntityManager)를 거치지 않고 SQL을 직접 실행한다.
     */
    private final JdbcTemplate jdbcTemplate;
    private final InfoAddService infoAddService;

    /**
     * 한 번에 DB로 전달할 최대 행 수(청크 크기).
     *
     * 리스트 전체를 한 번에 넘기면 PreparedStatement 버퍼가 과도하게 커질 수 있다.
     * BATCH_SIZE 단위로 분할해 batchUpdate를 호출하면 메모리 사용량을 제어할 수 있다.
     * rewriteBatchedStatements=true 환경에서는 청크 단위로 패킷이 묶인다.
     */
    private static final int BATCH_SIZE = 500;

    // ── TcgPPrice ─────────────────────────────────────────────────────────────

    /**
     * TCGPlayer 가격 데이터를 UPSERT한다.
     *
     * INSERT(신규 행) : codeNumber, printType, checkCode 등 파생 필드까지 모두 저장.
     * UPDATE(중복 행) : marketPrice만 갱신.
     * → 카드 이름·세트 등 메타데이터는 자주 바뀌지 않으므로 매번 덮어쓰지 않는다.
     * 정정이 필요하면 해당 행을 삭제 후 재동기화한다.
     *
     * UNIQUE KEY: (product_id, card_condition, printing, product_name)
     */
    @Transactional
    public void saveTcgBatch(List<TcgPPrice> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }

        incoming.forEach(this::enrichTcgDerivedFields);
        markDuplicateTcgCheckCodes(incoming);

        String sql = """
                INSERT INTO tcg_p_prices
                    (product_id, card_condition, game, is_supplemental, market_price,
                     number, printing, product_name, rarity, set_name, set_abbrv, type,
                     print_type, is_double_sided, downloaded, code_number, check_code, check_code_refined, ob_price_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    market_price = VALUES(market_price)
                """;

        for (List<TcgPPrice> chunk : partition(incoming, BATCH_SIZE)) {
            List<Object[]> params = chunk.stream()
                    .filter(p -> p != null && p.getProductId() != null)
                    .map(p -> new Object[] {
                            p.getProductId(),
                            p.getCondition(),
                            p.getGame(),
                            p.getIsSupplemental(),
                            normalizeMarketPrice(p.getMarketPrice()),
                            p.getNumber(),
                            p.getPrinting(),
                            p.getProductName(),
                            p.getRarity(),
                            p.getSet(),
                            p.getSetAbbrv(),
                            p.getType(),
                            p.getPrintType(),
                            p.getIsDoubleSided(),
                            p.getDownloaded(),
                            p.getCodeNumber(),
                            p.getCheckCode(),
                            p.getCheckCodeRefined(),
                            p.getObPriceId()
                    })
                    .toList();
            jdbcTemplate.batchUpdate(sql, params);
        }
    }

    // ── MtgPrice ──────────────────────────────────────────────────────────────

    /**
     * OpenBinder MTG 가격 데이터를 UPSERT한다.
     *
     * INSERT(신규 행) : set_code, code, type, price, set_name, name, name_k, rarity,
     * check_code 저장.
     * UPDATE(중복 행) : price, name, name_k 갱신.
     * price는 COALESCE(신규, 기존)으로 처리해 신규 값이 NULL이면 기존 가격을 유지한다.
     * name / name_k도 COALESCE로 처리해 신규 값이 NULL·빈 문자열이면 기존 값을 유지한다.
     * → tcg_p_price_id, con_num_name은 PriceLinkService가 별도로 연결하므로
     * 여기서 건드리지 않는다. ON DUPLICATE KEY UPDATE에 포함하면 덮어써진다.
     *
     * UNIQUE KEY: (set_code, code, type)
     * 갱신된 name / name_k는 UnionPrice 인제스션(card_name / card_namek)에 반영된다.
     */
    @Transactional
    public void saveMtgBatch(List<MtgPrice> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }

        markDuplicateMtgCheckCodes(incoming);

        String sql = """
                INSERT INTO mtg_prices
                    (set_code, code, type, price, set_name, name, name_k, rarity, downloaded, check_code, check_code_refined)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    price = COALESCE(VALUES(price), price),
                    name = COALESCE(NULLIF(VALUES(name), ''), name),
                    name_k = COALESCE(NULLIF(VALUES(name_k), ''), name_k)
                """;

        for (List<MtgPrice> chunk : partition(incoming, BATCH_SIZE)) {
            List<Object[]> params = chunk.stream()
                    .map(m -> new Object[] {
                            m.getSet(),
                            m.getCode(),
                            m.getType(),
                            normalizeMarketPrice(m.getPrice()),
                            m.getSetName(),
                            m.getName(),
                            m.getNameK(),
                            m.getRarity(),
                            Boolean.FALSE,
                            m.getCheckCode(),
                            m.getCheckCodeRefined()
                    })
                    .toList();
            jdbcTemplate.batchUpdate(sql, params);
        }
    }

    // ── FabPrice ──────────────────────────────────────────────────────────────

    /**
     * OpenBinder FAB 가격 데이터를 UPSERT한다.
     *
     * INSERT(신규 행) : 모든 필드 저장.
     * UPDATE(중복 행) : price만 갱신.
     * price는 COALESCE(신규, 기존)으로 처리해 신규 값이 NULL이면 기존 가격을 유지한다.
     * 메타데이터·check_code 정정이 필요하면 행 삭제 후 재동기화한다.
     * → tcg_p_price_id, con_num_name은 PriceLinkService가 별도로 연결하므로 건드리지 않는다.
     *
     * UNIQUE KEY: (set_code, code)
     */

    private boolean isValidCode(String code) {
        return code != null
                && code.contains("-")
                && !code.contains("-RF")
                && !code.contains("-FUN")
                && !code.contains("-CF")
                && !code.contains("-CC")
                && !code.contains("-JAN")
                && !code.contains("-FRC"); // 프랑스어 버전 제외
    }

    // private boolean isValidRarity(String rarity) {
    // return rarity != null
    // && !rarity.contains("Token");
    // }

    @Transactional
    public void saveFabBatch(List<FabPrice> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return;
        }

        List<FabPrice> filteredRightCode = new ArrayList<>();
        for (FabPrice price : incoming) {
            if ((isValidCode(price.getCode()))) {
                filteredRightCode.add(price);
            }
        }
        markDuplicateFabCheckCodes(filteredRightCode);

        String sql = """
                INSERT INTO fab_prices
                    (set_code, code, price, rarity, set_name, card_name, collector_num, foil, downloaded, check_code, check_code_refined)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    price = COALESCE(VALUES(price), price)
                """;

        for (List<FabPrice> chunk : partition(filteredRightCode, BATCH_SIZE)) {
            List<Object[]> params = chunk.stream()
                    .map(f -> new Object[] {
                            f.getSet(),
                            f.getCode(),
                            normalizeMarketPrice(f.getPrice()),
                            f.getRarity(),
                            f.getSetName(),
                            f.getCardName(),
                            f.getCollectorNum(),
                            f.getFoil(),
                            Boolean.FALSE,
                            f.getCheckCode(),
                            f.getCheckCodeRefined()
                    })
                    .toList();
            jdbcTemplate.batchUpdate(sql, params);
        }
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    /**
     * 배치 내에서 check_code_refined 값이 중복된 MtgPrice 항목을 감지해
     * 해당 항목의 check_code_refined를 DUPLICATE_CODE로 교체한다.
     */
    private void markDuplicateMtgCheckCodes(List<MtgPrice> list) {
        Set<String> duplicates = list.stream()
                .map(MtgPrice::getCheckCodeRefined)
                .filter(cr -> cr != null && !cr.isBlank() && !DUPLICATE_CODE.equals(cr))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (duplicates.isEmpty()) {
            return;
        }

        log.warn("MTG batch: check_code_refined 중복 {}건 감지 → DUPLICATE_CODE 처리: {}",
                duplicates.size(), duplicates);
        list.forEach(m -> {
            if (duplicates.contains(m.getCheckCodeRefined())) {
                m.setCheckCodeRefined(DUPLICATE_CODE);
            }
        });
    }

    /**
     * 배치 내에서 check_code_refined 값이 중복된 FabPrice 항목을 감지해
     * 해당 항목의 check_code_refined를 DUPLICATE_CODE로 교체한다.
     *
     * JOIN 대상인 tcg_p_prices.check_code는 절대 DUPLICATE_CODE 값을 갖지 않으므로
     * 이 플레이스홀더가 들어간 행은 overlay UPDATE에서 자동으로 제외된다.
     */
    private void markDuplicateFabCheckCodes(List<FabPrice> list) {
        Set<String> duplicates = list.stream()
                .map(FabPrice::getCheckCodeRefined)
                .filter(cr -> cr != null && !cr.isBlank() && !DUPLICATE_CODE.equals(cr))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (duplicates.isEmpty()) {
            return;
        }

        log.warn("FAB batch: check_code_refined 중복 {}건 감지 → DUPLICATE_CODE 처리: {}",
                duplicates.size(), duplicates);
        list.forEach(f -> {
            if (duplicates.contains(f.getCheckCodeRefined())) {
                f.setCheckCodeRefined(DUPLICATE_CODE);
            }
        });
    }

    /**
     * TcgPPrice의 파생 필드를 채운다.
     *
     * 원본 필드(game, number, setAbbrv, printing)에서 아래를 계산한다.
     * - codeNumber : "세트코드-카드번호" 형식의 인덱스용 코드
     * - printType : "Foil" / "Normal" / "Unknown"
     * - checkCode : mtg_prices 또는 fab_prices와 연결하기 위한 매칭 키
     *
     * UPSERT 특성상 신규·기존 구분 없이 모든 행에 미리 적용한다.
     * 기존 행의 경우 ON DUPLICATE KEY UPDATE가 market_price만 갱신하므로
     * 이 파생값들은 INSERT 절에서 제공되더라도 실제로는 DB에 반영되지 않는다.
     */
    private void enrichTcgDerivedFields(TcgPPrice price) {
        price.setCodeNumber(
                infoAddService.generateCodeNumber(price));
        price.setPrintType(infoAddService.getPrintType(price.getPrinting()));

        switch (price.getGame() == null ? "" : price.getGame()) {
            case "Magic" -> {
                String key = infoAddService.buildCheckCodeMtgForTcgP(price);
                price.setCheckCode(key);
                String productType = price.getType() == null ? "" : price.getType();
                price.setCheckCodeRefined(productType.contains("Sealed Products") ? "Sealed Products" : key);
            }
            case "Flesh & Blood TCG" -> {
                String key = infoAddService.buildCheckCodeFabForTcgP(price);
                price.setCheckCode(key);
                price.setCheckCodeRefined(key);
            }
            case "Star Wars Unlimited" -> {
                String key = infoAddService.buildCheckCodeForSWU(price);
                price.setCheckCode(key);
                price.setCheckCodeRefined(key);
            }
            case "Lorcana TCG" -> {
                String key = infoAddService.buildCheckCodeForLorcana(price);
                price.setCheckCode(key);
                price.setCheckCodeRefined(key);
            }
            case "Riftbound League of Legends Trading Card Game" -> {
                String key = infoAddService.buildCheckCodeForRift(price);
                price.setCheckCode(key);
                price.setCheckCodeRefined(key);
            }
            default -> {

            }
        }
    }

    /**
     * 배치 내에서 check_code_refined 값이 중복된 TcgPPrice 항목을 감지해
     * 해당 항목의 check_code_refined를 NULL로 교체한다.
     *
     * NULL이면 overlay JOIN(`t.check_code_refined = f.check_code_refined`)에서
     * 자동으로 제외되어 비결정적 다중 업데이트를 방지한다.
     */
    private void markDuplicateTcgCheckCodes(List<TcgPPrice> list) {
        Set<String> duplicates = list.stream()
                .map(TcgPPrice::getCheckCodeRefined)
                .filter(cr -> cr != null && !cr.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (duplicates.isEmpty()) {
            return;
        }

        log.warn("TCG batch: check_code_refined 중복 {}건 감지 → NULL 처리: {}",
                duplicates.size(), duplicates);
        list.forEach(p -> {
            if (duplicates.contains(p.getCheckCodeRefined())) {
                p.setCheckCodeRefined(null);
            }
        });
    }

    /**
     * BigDecimal의 부동소수점 표현 오차를 제거한다.
     *
     * BigDecimal(double)로 생성하면 "0.10000000000000001" 같은 값이 생길 수 있다.
     * toString() 후 다시 BigDecimal로 파싱하면 "0.1"처럼 정규화된다.
     * toPlainString()은 "1E+2" 대신 "100" 형식으로 지수 표기를 방지한다.
     */
    private static BigDecimal normalizeMarketPrice(BigDecimal raw) {
        if (raw == null) {
            return null;
        }
        return new BigDecimal(raw.toPlainString());
    }

    /**
     * 리스트를 size 크기의 청크(부분 리스트)로 분할한다.
     *
     * List.subList()는 원본 리스트의 뷰(view)를 반환하므로
     * 데이터 복사 없이 O(1)로 슬라이싱된다.
     *
     * 예) size=500, list.size()=1300 → 청크 3개 [0~499], [500~999], [1000~1299]
     */
    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            chunks.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return chunks;
    }
}
