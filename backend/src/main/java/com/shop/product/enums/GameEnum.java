package com.shop.product.enums;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public enum GameEnum {

    MTG(1L, "Magic: The Gathering", "mtg", "Magic: The Gathering"),
    LORC(71L, "Disney Lorcana", "lorc", "Lorcana TCG"),
    SWU(79L, "Star Wars: Unlimited", "swu", "Star Wars Unlimited"),
    FAB(62L, "Flesh and Blood TCG", "fab", "Flesh & Blood TCG"),
    RIFT(89L, "Riftbound: League of Legends Trading Card Game", "rift", "Riftbound League of Legends Trading Card Game"),
    OTHER(0L, "Other", "other", "Other");

    /**
     * 고객 검색·facet·자동완성에서 임시 제외할 {@code product_search_maps.game} 값.
     * 헤더 네비 LORC 숨김과 동일 정책 — 공개 시 비우거나 제거.
     */
    private static final List<String> TEMPORARILY_HIDDEN_FROM_CUSTOMER_SEARCH = List.of(
            LORC.game,
            LORC.productLineName);

    private final Long productId;
    private final String productLineName;
    private final String gameAbbr;
    private final String game;

    GameEnum(Long productId, String productLineName, String gameAbbr, String game) {
        this.productId = productId;
        this.productLineName = productLineName;
        this.gameAbbr = gameAbbr;
        this.game = game;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductLineName() {
        return productLineName;
    }

    public String getGameAbbr() {
        return gameAbbr;
    }

    public String getGame() {
        return game;
    }

    public static GameEnum fromCode(String code) {
        if (code == null) {
            return OTHER;
        }

        for (GameEnum game : values()) {
            if (game.gameAbbr.equalsIgnoreCase(code.trim())) {
                return game;
            }
        }

        return OTHER;
    }

    public static Optional<GameEnum> fromProductLineId(Long productLineId) {
        if (productLineId == null) {
            return Optional.empty();
        }
        for (GameEnum game : values()) {
            if (game != OTHER && game.productId.equals(productLineId)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    /**
     * DB에 섞여 있는 게임명(GameEnum.game / productLineName / gameAbbr)을 매칭한다.
     * 예: "Riftbound: League of Legends Trading Card Game" → RIFT
     */
    public static Optional<GameEnum> fromName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        for (GameEnum value : values()) {
            if (value == OTHER) {
                continue;
            }
            if (value.game.equalsIgnoreCase(trimmed)
                    || value.productLineName.equalsIgnoreCase(trimmed)
                    || value.gameAbbr.equalsIgnoreCase(trimmed)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * {@code price_config.config_game} 조회용 정식 게임명.
     * 별칭이면 {@link #getGame()}으로 정규화하고, 매칭 실패 시 원문을 유지한다.
     */
    public static String toConfigGame(String name) {
        if (name == null) {
            return null;
        }
        return fromName(name).map(GameEnum::getGame).orElse(name);
    }

    /**
     * 고객 검색 필터용: 선택한 게임명의 별칭({@link #game} / {@link #productLineName} / {@link #gameAbbr})을 모두 포함한다.
     * 예: 밀봉 상품이 TCGPlayer productLineName(콜론 있음)으로 저장돼 있어도 정식 game 명으로 조회된다.
     */
    public static List<String> expandSearchNames(List<String> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            expanded.add(trimmed);
            fromName(trimmed).ifPresent(game -> {
                expanded.add(game.getGame());
                expanded.add(game.getProductLineName());
                expanded.add(game.getGameAbbr());
            });
        }
        return new ArrayList<>(expanded);
    }

    public static List<String> temporarilyHiddenFromCustomerSearchGameNames() {
        return TEMPORARILY_HIDDEN_FROM_CUSTOMER_SEARCH;
    }
}
