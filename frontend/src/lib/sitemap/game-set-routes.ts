import type { MetadataRoute } from "next";

import { TCGP_GAME_SET_SOURCES } from "@/config/game-product-lines";
import { getServerApiBaseUrl } from "@/lib/server-api";
import type { MtgSetInfoDto, TcgPSetInfoDto } from "@/types/tcgMetadata";

import { buildLocaleUrl } from "./locale-url";

const SITEMAP_FETCH_REVALIDATE = 86400;
const SITEMAP_FETCH_TIMEOUT_MS = 10_000;

type SetPathSegment = { game: string; segment: string };

async function fetchJson<T>(url: string): Promise<T | null> {
  try {
    const res = await fetch(url, {
      next: { revalidate: SITEMAP_FETCH_REVALIDATE },
      signal: AbortSignal.timeout(SITEMAP_FETCH_TIMEOUT_MS),
    });
    if (!res.ok) return null;
    return (await res.json()) as T;
  } catch {
    return null;
  }
}

async function collectSetPathSegments(): Promise<SetPathSegment[]> {
  const base = getServerApiBaseUrl();
  const segments: SetPathSegment[] = [];

  const mtgSets = await fetchJson<MtgSetInfoDto[]>(`${base}/api/mtg/sets`);
  if (Array.isArray(mtgSets)) {
    for (const set of mtgSets) {
      if (set.setCode) {
        segments.push({ game: "mtg", segment: set.setCode });
      }
    }
  }

  await Promise.all(
    TCGP_GAME_SET_SOURCES.map(async ({ game, productLineId }) => {
      const sets = await fetchJson<TcgPSetInfoDto[]>(`${base}/api/game/sets/${productLineId}`);
      if (!Array.isArray(sets)) return;
      for (const set of sets) {
        if (set.urlName) {
          segments.push({ game, segment: set.urlName });
        }
      }
    })
  );

  return segments;
}

export async function buildGameSetSitemapEntries(
  locales: readonly string[]
): Promise<MetadataRoute.Sitemap> {
  const setPaths = await collectSetPathSegments();
  const lastModified = new Date();

  return locales.flatMap((locale) =>
    setPaths.map(({ game, segment }) => ({
      url: buildLocaleUrl(locale, `/game/${game}/${encodeURIComponent(segment)}`),
      lastModified,
      changeFrequency: "weekly" as const,
      priority: 0.75,
    }))
  );
}
