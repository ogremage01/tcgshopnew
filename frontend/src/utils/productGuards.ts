export function toNonNegativeInt(n: unknown): number {
    const x = Number(n);
    if (!Number.isFinite(x) || x < 0) return 0;
    return Math.floor(x);
}

export function toSafePositiveIntId(n: unknown): number | null {
    const x = Number(n);
    if (!Number.isInteger(x) || !Number.isSafeInteger(x) || x <= 0) return null;
    return x;
}

export function parseQuantityFromInput(raw: string, maxStock: number): number | null {
    const s = raw.trim();
    if (s === "" || s === "-") return null;
    const v = Number(s);
    if (!Number.isFinite(v)) return null;
    const t = Math.trunc(v);
    if (t < 1) return 1;
    if (t > maxStock) return maxStock;
    return t;
}

export function clampQuantity(q: number, maxStock: number): number {
    if (!Number.isFinite(q) || !Number.isFinite(maxStock) || maxStock < 1) return 1;
    return Math.max(1, Math.min(Math.trunc(q), maxStock));
}

export function getProductListItemKey(product: {
    productType?: string;
    card?: { publicId?: string };
    manualProductInfoDto?: { searchMapId?: number; publicId?: string };
    sealedProductInfoDto?: { publicId?: string };
    supplyProductInfoDto?: { searchMapId?: number; publicId?: string };
    tableId?: number;
}, index: number): string {
    const supply = product.supplyProductInfoDto;
    if (supply?.searchMapId != null) return `supply-${supply.searchMapId}`;
    const manual = product.manualProductInfoDto;
    if (manual?.searchMapId != null) return `manual-${manual.searchMapId}`;
    if (product.sealedProductInfoDto?.publicId) {
        return `sealed-${product.sealedProductInfoDto.publicId}`;
    }
    if (product.card?.publicId) return `card-${product.card.publicId}`;
    if (product.tableId != null) return `table-${product.tableId}`;
    return `product-${index}`;
}
