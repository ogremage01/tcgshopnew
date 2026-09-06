package com.shop.product.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameEnumTest {

    @Test
    @DisplayName("Riftbound productLineName(콜론 있음)도 정식 game 명으로 정규화한다")
    void toConfigGame_riftboundProductLineName() {
        assertThat(GameEnum.toConfigGame("Riftbound: League of Legends Trading Card Game"))
                .isEqualTo(GameEnum.RIFT.getGame());
        assertThat(GameEnum.toConfigGame("Riftbound League of Legends Trading Card Game"))
                .isEqualTo(GameEnum.RIFT.getGame());
        assertThat(GameEnum.toConfigGame("rift"))
                .isEqualTo(GameEnum.RIFT.getGame());
    }

    @Test
    @DisplayName("fromName은 정식명·productLineName·약어를 모두 매칭한다")
    void fromName_matchesAliases() {
        assertThat(GameEnum.fromName("Riftbound: League of Legends Trading Card Game"))
                .contains(GameEnum.RIFT);
        assertThat(GameEnum.fromName("Star Wars: Unlimited")).contains(GameEnum.SWU);
        assertThat(GameEnum.fromName("Disney Lorcana")).contains(GameEnum.LORC);
        assertThat(GameEnum.fromName("Flesh and Blood TCG")).contains(GameEnum.FAB);
        assertThat(GameEnum.fromName("rift")).contains(GameEnum.RIFT);
        assertThat(GameEnum.fromName("unknown")).isEmpty();
    }

    @Test
    @DisplayName("매칭되지 않는 이름은 원문을 유지한다")
    void toConfigGame_unknownKeepsOriginal() {
        assertThat(GameEnum.toConfigGame("other")).isEqualTo("other");
        assertThat(GameEnum.toConfigGame(null)).isNull();
    }

    @Test
    @DisplayName("검색 필터는 Riftbound 정식명과 productLineName(콜론)을 함께 포함한다")
    void expandSearchNames_riftboundIncludesProductLineName() {
        assertThat(GameEnum.expandSearchNames(
                List.of("Riftbound League of Legends Trading Card Game")))
                .contains(
                        GameEnum.RIFT.getGame(),
                        GameEnum.RIFT.getProductLineName(),
                        GameEnum.RIFT.getGameAbbr());
        assertThat(GameEnum.expandSearchNames(
                List.of("Riftbound: League of Legends Trading Card Game")))
                .contains(
                        GameEnum.RIFT.getGame(),
                        GameEnum.RIFT.getProductLineName());
    }

    @Test
    @DisplayName("검색 필터 입력이 비어 있으면 그대로 반환한다")
    void expandSearchNames_emptyStaysEmpty() {
        assertThat(GameEnum.expandSearchNames(List.of())).isEmpty();
        assertThat(GameEnum.expandSearchNames(null)).isNull();
    }
}
