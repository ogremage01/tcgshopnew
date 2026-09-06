import type { MetadataRoute } from "next";

import { buildLocaleUrl } from "./locale-url";

type StaticRouteDef = {
  path: string;
  changeFrequency: MetadataRoute.Sitemap[number]["changeFrequency"];
  priority: number;
};

// TODO: LORC 추가 필요-추후 언락할 때
const PUBLIC_STATIC_ROUTES: StaticRouteDef[] = [
  { path: "/", changeFrequency: "daily", priority: 1 },
  { path: "/search", changeFrequency: "daily", priority: 0.9 },
  { path: "/special/products", changeFrequency: "weekly", priority: 0.8 },
  { path: "/game/mtg", changeFrequency: "weekly", priority: 0.85 },
  { path: "/game/fab", changeFrequency: "weekly", priority: 0.85 },
  { path: "/game/swu", changeFrequency: "weekly", priority: 0.85 },
  // { path: "/game/lorc", changeFrequency: "weekly", priority: 0.85 },
  { path: "/game/rift", changeFrequency: "weekly", priority: 0.85 },
];

export function buildStaticSitemapEntries(
  locales: readonly string[],
): MetadataRoute.Sitemap {
  const lastModified = new Date();
  return locales.flatMap((locale) =>
    PUBLIC_STATIC_ROUTES.map((route) => ({
      url: buildLocaleUrl(locale, route.path),
      lastModified,
      changeFrequency: route.changeFrequency,
      priority: route.priority,
    })),
  );
}
