import { api } from "@/lib/api";
import { GAME_ENUM, type GameEnumEntry } from "@/config/gameEnum";
import type {
  FabSetInfoDto,
  MtgSetInfoDto,
  TcgPSetInfoDto,
} from "@/types/tcgMetadata";

export type GameSetInfoDto = MtgSetInfoDto | FabSetInfoDto | TcgPSetInfoDto;

export function gameEntryByProductId(
  productId: number | string | null | undefined,
): GameEnumEntry | undefined {
  if (productId == null || productId === "") {
    return undefined;
  }
  const id = typeof productId === "string" ? Number(productId) : productId;
  if (!Number.isFinite(id) || id <= 0) {
    return undefined;
  }
  return GAME_ENUM.find((game) => game.productId === id);
}

export function setListPathForGame(
  game: Pick<GameEnumEntry, "gameAbbr" | "productId">,
): string {
  switch (game.gameAbbr) {
    case "mtg":
      return "/api/mtg/sets";
    case "fab":
      return "/api/admin/product/metadata/fab/sets";
    default:
      return `/api/admin/product/metadata/list/${game.productId}`;
  }
}

export function fetchSetListForGame(
  game: Pick<GameEnumEntry, "gameAbbr" | "productId">,
): Promise<GameSetInfoDto[]> {
  return api.get<GameSetInfoDto[]>(setListPathForGame(game));
}
