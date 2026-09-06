import { GAME_ENUM } from "@/config/gameEnum";

/** `/game/{code}/sealed` 밀봉 제품 목록을 제공하는 게임 코드 */
export type GameSealedBrowseCode = "mtg" | "fab" | "swu" | "rift";

export type GameSealedBrowseConfig = {
  /** product_search_maps.game 필터 값(정식명). 별칭 확장은 서버가 담당한다. */
  gameFilter: string;
  /** game.sealed.{key} 번역 namespace 키 */
  translationKey: GameSealedBrowseCode;
};

function sealedGameFilter(abbr: GameSealedBrowseCode): string {
  return GAME_ENUM.find((item) => item.gameAbbr === abbr)?.game ?? "";
}

export const GAME_SEALED_BROWSE_CONFIG: Record<
  GameSealedBrowseCode,
  GameSealedBrowseConfig
> = {
  mtg: {
    gameFilter: sealedGameFilter("mtg"),
    translationKey: "mtg",
  },
  fab: {
    gameFilter: sealedGameFilter("fab"),
    translationKey: "fab",
  },
  swu: {
    gameFilter: sealedGameFilter("swu"),
    translationKey: "swu",
  },
  rift: {
    gameFilter: sealedGameFilter("rift"),
    translationKey: "rift",
  },
};

export const GAME_SEALED_BROWSE_CODES = Object.keys(
  GAME_SEALED_BROWSE_CONFIG,
) as GameSealedBrowseCode[];

export function isGameSealedBrowseCode(
  code: string,
): code is GameSealedBrowseCode {
  return Object.hasOwn(GAME_SEALED_BROWSE_CONFIG, code);
}
