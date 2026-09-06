import type { CardProductLanguageDto } from "@/types/product";

export const FALLBACK_CARD_PRODUCT_LANGUAGES: CardProductLanguageDto[] = [
  { code: "en", displayName: "English", displayNameKo: "영어" },
  { code: "ko", displayName: "Korean", displayNameKo: "한글" },
];

export function normalizeLanguageCode(code?: string | null) {
  return (code ?? "").trim().toLowerCase();
}

export function compareLanguageCode(a?: string | null, b?: string | null) {
  const left = normalizeLanguageCode(a);
  const right = normalizeLanguageCode(b);
  const leftPriority = languagePriority(left);
  const rightPriority = languagePriority(right);
  if (leftPriority !== rightPriority) return leftPriority - rightPriority;
  return left.localeCompare(right);
}

export function languageDisplayName(
  code: string,
  locale = "en",
  languages: CardProductLanguageDto[] = FALLBACK_CARD_PRODUCT_LANGUAGES,
) {
  const normalizedLocale = normalizeLanguageCode(locale);
  const normalized = normalizeLanguageCode(code);
  const language = languages.find(
    (item) => normalizeLanguageCode(item.code) === normalized,
  );

  if (!language) return normalized.toUpperCase();

  if (normalizedLocale === "ko") {
    return language.displayNameKo || language.displayName;
  }

  return language.displayName;
}

export function languageShortLabel(code: string) {
  return normalizeLanguageCode(code).toUpperCase();
}

export function sortSaleMapEntries<T>(
  saleMap?: Record<string, T[] | undefined> | null,
) {
  return Object.entries(saleMap ?? {})
    .map(([code, sales]) => ({
      code: normalizeLanguageCode(code),
      sales: sales ?? [],
    }))
    .filter((entry) => entry.code && entry.sales.length > 0)
    .sort((a, b) => compareLanguageCode(a.code, b.code));
}

export function languageImageCandidates(
  imageMap: Record<string, string | null | undefined> | undefined,
  language: string | null | undefined,
  englishFallback?: string | null,
) {
  const normalized = normalizeLanguageCode(language);
  return [
    normalized ? imageMap?.[normalized] : undefined,
    imageMap?.en,
    englishFallback,
  ];
}

function languagePriority(code: string) {
  if (code === "en") return 0;
  if (code === "ko") return 1;
  return 2;
}
