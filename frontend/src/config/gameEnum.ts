/**
 * 백엔드 GameEnum.java 미러
 * gameAbbr — API 경로 및 URL에 사용 (예: "mtg")
 * displayAbbr — 관리자 목록 등 짧은 표시명
 * game      — DB/API 풀네임, price_config.config_game 에 저장되는 값
 * productLineName — TCGPlayer product line 공식명 (콜론 유무 등이 다를 수 있음)
 */
export const GAME_ENUM = [
  { productId: 1,  gameAbbr: "mtg",  displayAbbr: "MTG",      game: "Magic: The Gathering",                              productLineName: "Magic: The Gathering" },
  { productId: 71, gameAbbr: "lorc", displayAbbr: "LOR",      game: "Lorcana TCG",                                        productLineName: "Disney Lorcana" },
  { productId: 79, gameAbbr: "swu",  displayAbbr: "STARWARS", game: "Star Wars Unlimited",                                productLineName: "Star Wars: Unlimited" },
  { productId: 62, gameAbbr: "fab",  displayAbbr: "FAB",      game: "Flesh & Blood TCG",                                  productLineName: "Flesh and Blood TCG" },
  { productId: 89, gameAbbr: "rift", displayAbbr: "RIFT",     game: "Riftbound League of Legends Trading Card Game",      productLineName: "Riftbound: League of Legends Trading Card Game" },
] as const;

export type GameEnumEntry = (typeof GAME_ENUM)[number];
export type GameAbbrType = GameEnumEntry["gameAbbr"];
export type GameFullNameType = GameEnumEntry["game"];

/**
 * DB/폼에 섞여 있는 게임명(game / productLineName / gameAbbr)을 매칭한다.
 * 예: "Riftbound: League of Legends Trading Card Game" → RIFT
 */
export function fromGameName(
  name: string | null | undefined,
): GameEnumEntry | undefined {
  if (name == null || name.trim() === "") {
    return undefined;
  }
  const trimmed = name.trim().toLowerCase();
  return GAME_ENUM.find(
    (g) =>
      g.game.toLowerCase() === trimmed ||
      g.productLineName.toLowerCase() === trimmed ||
      g.gameAbbr.toLowerCase() === trimmed,
  );
}

/** GameEnum.game·productLineName → gameAbbr */
export const GAME_FULL_TO_ABBR: Record<string, GameAbbrType> = Object.fromEntries(
  GAME_ENUM.flatMap((g) => {
    const pairs: [string, GameAbbrType][] = [[g.game, g.gameAbbr]];
    if (g.productLineName !== g.game) {
      pairs.push([g.productLineName, g.gameAbbr]);
    }
    return pairs;
  }),
) as Record<string, GameAbbrType>;

/** gameAbbr → GameEnum.game 풀네임 */
export const GAME_ABBR_TO_FULL: Record<GameAbbrType, GameFullNameType> =
  Object.fromEntries(GAME_ENUM.map((g) => [g.gameAbbr, g.game])) as Record<GameAbbrType, GameFullNameType>;
