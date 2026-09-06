import { getSiteUrl } from "./site-url";

/** locale prefix가 항상 있는 경로용 절대 URL (path는 `/` 또는 `/search` 형태) */
export function buildLocaleUrl(locale: string, path: string): string {
  const siteUrl = getSiteUrl().replace(/\/$/, "");
  const normalized = path === "/" || path === "" ? "" : path.startsWith("/") ? path : `/${path}`;
  return `${siteUrl}/${locale}${normalized}`;
}
