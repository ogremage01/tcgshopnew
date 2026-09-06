"use client"
import type { SealedProductSaleDto } from "@/types/product";
import { toNonNegativeInt, toSafePositiveIntId } from "@/utils/productGuards";
import ProductQuantityButton from "../ProductQuantityButton";
import ProductStockPriceRow from "../ProductStockPriceRow";
export default function SealedProductRow({ sale, handleAddToCart }: { sale: SealedProductSaleDto, handleAddToCart: (searchMapId: number, quantity: number) => void }) {
    const maxStock = toNonNegativeInt(sale?.currentVisibleStock);
    const safePrice = Number.isFinite(sale?.showingPrice) ? sale.showingPrice : null;
    const searchMapId = toSafePositiveIntId(sale?.searchMapId);
    return (
        <div className="flex flex-col gap-1">
            <ProductStockPriceRow
                stock={sale.currentVisibleStock}
                priceLabel={safePrice == null ? "—" : `₩ ${safePrice.toLocaleString()}`}
            />
            {maxStock > 0 && (
            <ProductQuantityButton
                onAddToCart={(quantity) => {
                    if (searchMapId == null) return;
                    handleAddToCart(searchMapId, quantity);
                }}
                maxStock={maxStock}
                disabled={searchMapId == null || maxStock < 1}
                resetKey={searchMapId}
                layout="grid"
            />
            )}
        </div>
    )
}
