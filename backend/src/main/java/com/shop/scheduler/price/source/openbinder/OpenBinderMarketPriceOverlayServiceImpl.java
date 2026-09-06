package com.shop.scheduler.price.source.openbinder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenBinderMarketPriceOverlayServiceImpl implements OpenBinderMarketPriceOverlayService {

    private static final String FLESH_AND_BLOOD_GAME = "Flesh & Blood TCG";
    private static final String MAGIC_GAME = "Magic";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public int applyFabMarketPricesToTcgP() {
        List<String> setAbbrvList = fetchDistinctSetAbbrv(FLESH_AND_BLOOD_GAME);
        String overlaySql = """
                UPDATE tcg_p_prices t
                JOIN fab_prices f ON t.check_code_refined = f.check_code_refined
                SET t.market_price = f.price, t.ob_price_id = f.id
                WHERE t.game = ? AND t.set_abbrv <=> ? AND t.check_code_refined IS NOT NULL AND t.check_code_refined <> ''
                  AND f.price IS NOT NULL AND f.price > 0
                  AND (NOT (t.market_price <=> f.price) OR NOT (t.ob_price_id <=> f.id))
                """;
        int totalUpdated = 0;
        for (String setAbbrv : setAbbrvList) {
            totalUpdated += updateBySetAbbrv(overlaySql, FLESH_AND_BLOOD_GAME, setAbbrv);
        }
        return totalUpdated;
    }

    @Override
    public int applyMtgMarketPricesToTcgP() {
        List<String> setAbbrvList = fetchDistinctSetAbbrv(MAGIC_GAME);
        String overlaySql = """
                UPDATE tcg_p_prices t
                JOIN mtg_prices m ON t.check_code_refined = m.check_code_refined
                SET t.market_price = m.price, t.ob_price_id = m.id
                WHERE t.game = ? AND t.set_abbrv = ? AND t.check_code_refined IS NOT NULL AND t.check_code_refined <> ''
                  AND m.price IS NOT NULL AND m.price > 0
                  AND (NOT (t.market_price <=> m.price) OR NOT (t.ob_price_id <=> m.id))
                """;
        int totalUpdated = 0;
        for (String setAbbrv : setAbbrvList) {
            totalUpdated += updateBySetAbbrv(overlaySql, MAGIC_GAME, setAbbrv);
        }
        return totalUpdated;
    }

    private List<String> fetchDistinctSetAbbrv(String game) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT set_abbrv FROM tcg_p_prices WHERE game = ? AND set_abbrv IS NOT NULL AND set_abbrv <> ''",
                String.class, game);
    }

    @Transactional
    int updateBySetAbbrv(String sql, String game, String setAbbrv) {
        return jdbcTemplate.update(sql, game, setAbbrv);
    }
}
