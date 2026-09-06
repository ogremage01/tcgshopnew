import type { MetadataRoute } from "next";

import { routing } from "@/i18n/routing";
import { buildGameSetSitemapEntries } from "@/lib/sitemap/game-set-routes";
import { buildStaticSitemapEntries } from "@/lib/sitemap/static-routes";

export const revalidate = 86400;

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const locales = routing.locales;
  const [staticPages, gameSetPages] = await Promise.all([
    Promise.resolve(buildStaticSitemapEntries(locales)),
    buildGameSetSitemapEntries(locales),
  ]);

  return [...staticPages, ...gameSetPages];
}
