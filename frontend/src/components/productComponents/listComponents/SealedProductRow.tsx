"use client";
import type { SealedProductSaleDto } from "@/types/product";
import { toNonNegativeInt, toSafePositiveIntId } from "@/utils/productGuards";
import ProductQuantityButton from "../ProductQuantityButton";
import ProductListSaleRow from "../ProductListSaleRow";

export default function SealedProductRow({
  sale,
  handleAddToCart,
}: {
  sale: SealedProductSaleDto;
  handleAddToCart: (searchMapId: number, quantity: number) => void;
}) {
  const maxStock = toNonNegativeInt(sale?.currentVisibleStock);
  const safePrice = Number.isFinite(sale?.showingPrice) ? sale.showingPrice : null;
  const searchMapId = toSafePositiveIntId(sale?.searchMapId);
  const priceLabel =
    safePrice == null ? "—" : `₩ ${safePrice.toLocaleString()}`;

  return (
    <ProductListSaleRow
      priceLabel={priceLabel}
      stock={sale.currentVisibleStock}
      quantityButton={
        maxStock > 0 ? (
          <ProductQuantityButton
            layout="list"
            onAddToCart={(quantity) => {
              if (searchMapId == null) return;
              handleAddToCart(searchMapId, quantity);
            }}
            maxStock={maxStock}
            disabled={searchMapId == null || maxStock < 1}
            resetKey={searchMapId}
          />
        ) : undefined
      }
    />
  );
}
