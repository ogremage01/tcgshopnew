import type { CardProductSaleDto } from "@/types/product";

export function formatMoneyByLocale(
    _locale: string,
    krw: number | null | undefined,
    _usd?: number | null | undefined,
): string {
    const safePrice = Number.isFinite(krw) ? krw : null;
    return safePrice == null ? "—" : `₩ ${safePrice.toLocaleString()}`;
}

export function formatCardSaleShowingPrice(
    locale: string,
    sale: Pick<CardProductSaleDto, "showingPrice" | "showingPriceUsd">,
): string {
    return formatMoneyByLocale(locale, sale.showingPrice, sale.showingPriceUsd);
}

export function formatCheckoutAmount(
    locale: string,
    krw: number,
    _krwPerUsd?: number,
): string {
    return formatMoneyByLocale(locale, krw);
}
